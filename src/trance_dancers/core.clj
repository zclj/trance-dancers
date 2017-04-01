(ns trance-dancers.core
  (:require [clojure.core.async :as async]))

;; some dance business logic..
(defn get-down [moves]
  (conj moves :get-down))

(defn get-up [moves]
  (conj moves :get-up))

;; start a simulated, possibly remote, service that now how to make moves
(defn start-moves-engine [in-ch out-ch]
  (let [state (atom :running)]
   (async/go-loop []
     (when-let [[topic payload] (async/<! in-ch)]
       (try
         (cond
           (= topic :stop) (async/put! out-ch (reset! state :stopped))
           (= topic :up)
           (async/put! out-ch [:move-ok (get-up payload)])
           (= topic :down)
           (async/put! out-ch [:move-ok (get-down payload)])
           :else (async/put! out-ch [:move-error "unknown move"]))
         (catch Exception e
           (async/put! out-ch (str "Exception " (.getMessage e))))))
     (when (= @state :running)
       (recur)))))

;; start a simulated dance manager that knows how to compose dances by using move engines,
;; that might be remote. Every call has the potential to timeout
(defn start-dance-manager [in-ch out-ch moves-engine]
  (let [state (atom :running)]
   (async/go-loop []
     (when-let [[topic payload] (async/<! in-ch)]
       (try
         (cond
           (= topic :stop) (async/put! out-ch (reset! state :stopped))
           (= topic :do-the-choka-choka)
           ;; the chocka chocka is composed of a up move and a down move
           (let [[not-timeout ch] (async/alts! [[(:in-ch moves-engine)
                                                 [:up payload]]
                                                (async/timeout 1000)])]
             (if not-timeout
               (let [[result ch] (async/alts! [(:out-ch moves-engine)
                                               (async/timeout 1000)])]
                 (if result
                   (if (= (first result) :move-ok)
                     ;; if the up move was ok, lets do the down move
                     (let [[not-timeout ch] (async/alts! [[(:in-ch moves-engine)
                                                           [:down (second result)]]
                                                          (async/timeout 1000)])]
                       (if not-timeout
                         (let [[result ch] (async/alts! [(:out-ch moves-engine)
                                                         (async/timeout 1000)])]
                           (if result
                             (if (= (first result) :move-ok)
                               (async/put! out-ch [:dance-ok (second result)])
                               (async/put! out-ch [:dance-move-error result]))))))
                     (async/put! out-ch [:dance-move-error result]))
                   (async/put! out-ch [:timeout-move topic])))
               (async/put! out-ch [:timeout topic])))
           :else (async/put! out-ch [:dance-error "unknown dance"]))
         (catch Exception e (async/put! out-ch (str "Exception " (.getMessage e))))))
     (when (= @state :running)
       (recur)))))

(defn run-dance []
  (let [moves-engine-out-channel (async/chan)
        moves-engine-in-channel (async/chan)
        moves-engine {:in-ch moves-engine-in-channel :out-ch moves-engine-out-channel}
        dance-manager-out-channel (async/chan)
        dance-manager-in-channel (async/chan)]
    (start-moves-engine moves-engine-in-channel moves-engine-out-channel)
    (start-dance-manager dance-manager-in-channel dance-manager-out-channel moves-engine)
    
    ;; tell it do do an unknown dance
    (async/go (async/>! dance-manager-in-channel [:up :dude]))
    ;; should give error
    (println (async/<!! dance-manager-out-channel))

    ;; do the choka choka dance
    (async/go (async/>! dance-manager-in-channel [:do-the-choka-choka []]))
    (println (async/<!! dance-manager-out-channel))

    ;; stop the manager and engine
    (async/go (async/>! dance-manager-in-channel [:stop :dude]))
    (async/go (async/>! moves-engine-in-channel [:stop :dude]))

    ;; should give stop message
    (println (async/<!! dance-manager-out-channel))))

