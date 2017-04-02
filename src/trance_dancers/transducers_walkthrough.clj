(ns trance-dancers.transducers-walkthrough)

(defn transducer-factory-1 [reducing-fuction-like-conj]
  (fn
    ;; init
    ([] (reducing-fuction-like-conj))
    ;; completion
    ([result] (reducing-fuction-like-conj result))
    ;; step
    ([result input]
     (reducing-fuction-like-conj result input))))

;; from the implementation of into
;;(transduce xform conj to from)

;; transduce will call xform with the reducing function f to build
;; a new reducing function witch is then applied on the collection
(def my-init-value [1])
(def my-collection [2 3 4])
(def my-reducing-function conj)
(def my-reduced-result [1 2 3 4])
(transduce transducer-factory-1 my-reducing-function my-init-value my-collection)

(def the-produced-reducing-function (transducer-factory-1 conj))
;; calling the produced-reducing-function
;; 0 arity - 'init'
(= (the-produced-reducing-function) (conj))
;; 1 arity - 'completion'
(= (the-produced-reducing-function my-reduced-result) (conj [1 2 3 4]))
;; 2 arity - 'step', producing a single step given the next input
(= (the-produced-reducing-function my-init-value 2) (conj [1] 2))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; For reference, clojure.core implementations of refered functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; reducing function conj, from clojure.core

;; (def
;;   ^{:arglists '([coll x] [coll x & xs])
;;     :doc "conj[oin]. Returns a new collection with the xs
;;     'added'. (conj nil item) returns (item).  The 'addition' may
;;     happen at different 'places' depending on the concrete type."
;;     :added "1.0"
;;     :static true}
;;   conj (fn ^:static conj
;;          ([] [])
;;          ([coll] coll)
;;          ([coll x] (clojure.lang.RT/conj coll x))
;;          ([coll x & xs]
;;           (if xs
;;             (recur (clojure.lang.RT/conj coll x) (first xs) (next xs))
;;             (clojure.lang.RT/conj coll x)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; transduce from clojure.core

;; (defn transduce
;;   "reduce with a transformation of f (xf). If init is not
;;   supplied, (f) will be called to produce it. f should be a reducing
;;   step function that accepts both 1 and 2 arguments, if it accepts
;;   only 2 you can add the arity-1 with 'completing'. Returns the result
;;   of applying (the transformed) xf to init and the first item in coll,
;;   then applying xf to that result and the 2nd item, etc. If coll
;;   contains no items, returns init and f is not called. Note that
;;   certain transforms may inject or skip items."  {:added "1.7"}
;;   ([xform f coll] (transduce xform f (f) coll))
;;   ([xform f init coll]
;;    (let [f (xform f)
;;          ret (if (instance? clojure.lang.IReduceInit coll)
;;                (.reduce ^clojure.lang.IReduceInit coll f init)
;;                (clojure.core.protocols/coll-reduce coll f init))]
;;      (f ret))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; into from clojure.core

;; (defn into
;;   "Returns a new coll consisting of to-coll with all of the items of
;;   from-coll conjoined. A transducer may be supplied."
;;   {:added "1.0"
;;    :static true}
;;   ([] [])
;;   ([to] to)
;;   ([to from]
;;    (if (instance? clojure.lang.IEditableCollection to)
;;      (with-meta (persistent! (reduce conj! (transient to) from)) (meta to))
;;      (reduce conj to from)))
;;   ([to xform from]
;;    (if (instance? clojure.lang.IEditableCollection to)
;;      (with-meta (persistent! (transduce xform conj! (transient to) from)) (meta to))
;;      (transduce xform conj to from))))
