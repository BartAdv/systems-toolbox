(ns example.ui-mouse-svg
  (:require [reagent.core :as r :refer [atom]]
            [matthiasn.systems-toolbox.component :as comp]
            [matthiasn.systems-toolbox.helpers :refer [by-id]]
            [cljs.core.match :refer-macros [match]]))

(enable-console-print!)

(def circle-defaults {:fill "rgba(255,0,0,0.1)" :stroke "black" :stroke-width 2 :r 15})
(def text-default {:stroke "none" :fill "black" :style {:font-size 12}})
(def text-bold (merge text-default {:style {:font-weight :bold :font-size 12}}))
(def x-axis-label (merge text-default {:text-anchor :middle}))

(defn now [] (.getTime (js/Date.)))

(defn mouse-move-ev-handler
  "Handler function for mouse move events, triggered when mouse is moved above SVG. Sends timestamped
  coordinates to server."
  [app put-fn curr-cmp]
  (fn [ev]
    (let [rect (-> curr-cmp r/dom-node .getBoundingClientRect)
          pos {:x (- (.-clientX ev) (.-left rect)) :y (.toFixed (- (.-clientY ev) (.-top rect)) 0) :timestamp (now)}]
      (swap! app assoc :pos pos)
      (put-fn [:cmd/mouse-pos pos])
      (.stopPropagation ev))))

(def path-defaults {:fill :black :stroke :black :stroke-width 1})

(defn interquartile-range
  "Determines the interquartile range of values in a collection of numbers."
  [sample]
  (let [sorted (sort sample)
        n (count sorted)
        q1 (nth sorted (Math/floor (/ n 4)))
        q3 (nth sorted (Math/floor (* (/ n 4) 3)))
        iqr (- q3 q1)]
    iqr))

(defn freedman-diaconis-rule
  "Implements approximation of Freedman–Diaconis rule for determing bin size in histograms: bin size = 2 IQR(x) n^-1/3
   where IQR(x) is the interquartile range of the data and n is the number of observations in the sample x.
   Argument coll is expected to be a collection of numbers."
  [sample]
  (let [n (count sample)]
    (when (pos? n)
      (* 2 (interquartile-range sample) (Math/pow n (/ -1 3))))))

(defn tick-length
  "Determines length of tick for the chart axes."
  [n]
  (cond
    (zero? (mod n 100)) 9
    (zero? (mod n 50)) 6
    :else 3))

(defn x-axis
  "Draws x-axis of a chart."
  [x y l scale]
  [:g
   [:path (merge path-defaults {:d (str "M" x " " y "l" (* l scale) " 0 l 0 -4 l 10 4 l -10 4 l 0 -4 z")})]
   (for [n (range 0 l 10)]
     ^{:key (str "xt" n)} [:path (merge path-defaults {:d (str "M" (+ x (* n scale)) " " y "l 0 " (tick-length n))})])
   (for [n (range 0 l 50)]
     ^{:key (str "xl" n)} [:text (merge x-axis-label {:x (+ x (* n scale)) :y (+ y 20)}) n])])

(defn x-axis2
  "Draws x-axis of a chart."
  [x y l scale]
  [:g
   [:path (merge path-defaults {:d (str "M" x " " y "l" (* l scale) " 0 l 0 -4 l 10 4 l -10 4 l 0 -4 z")})]
   (for [n (range 0 l 10)]
     ^{:key (str "xt" n)} [:path (merge path-defaults {:d (str "M" (+ x (* n scale)) " " y "l 0 " (tick-length n))})])
   (for [n (range 0 l 50)]
     ^{:key (str "xl" n)} [:text (merge x-axis-label {:x (+ x (* n scale)) :y (+ y 20)}) n])])

(defn y-axis
  "Draws y-axis of a chart."
  [x y l scale]
  [:g
   [:path (merge path-defaults {:d (str "M" x " " y "l 0 " (* l scale -1) " l -4 0 l 4 -10 l 4 10 l -4 0 z")})]
   (for [n (range 0 l 10)]
     ^{:key (str "yt" n)} [:path (merge path-defaults
                                        {:d (str "M" x " " (- y (* n scale)) "l -" (tick-length n) " 0")})])
   (for [n (range 0 l 50)]
     ^{:key (str "yl" n)} [:text (merge text-default {:x (- x 10) :y (- y (* n scale) -4) :text-anchor :end}) n])])

(defn histogram-view
  "Renders a histogram for roundtrip times."
  [rtt-times x y w h]
  (let [freq (frequencies rtt-times)
        max-freq (apply max (map (fn [[_ f]] f) freq))
        scale 2
        mx (apply max rtt-times)
        x-axis-l (max (+ (* (Math/ceil (/ mx 50)) 50) 20) 130)
        y-axis-l (min (max (+ (* (Math/ceil (/ max-freq 10)) 10) 20) 70) 110)]
    [:g
     (for [[v f] freq]
       ^{:key (str "b" v f)} [:rect {:x (+ x (* v scale)) :y (- y (* f scale))
                                     :fill "steelblue" :width 1.3 :height (* f scale)}])
     [x-axis x y x-axis-l scale]
     [:text (merge x-axis-label text-bold {:x (+ x x-axis-l -10) :y (+ y 40)}) "Roundtrip t/ms"]
     [:text (let [x-coord (- x 45) y-coord (- y y-axis-l 10) rotate (str "rotate(270 " x-coord " " y-coord ")")]
              (merge x-axis-label text-bold {:x x-coord :y y-coord :transform rotate})) "Frequencies"]
     [y-axis x y y-axis-l scale]]))

(defn y-axis2
  "Draws y-axis of a chart."
  [x y l scale]
  [:g
   [:path (merge path-defaults {:d (str "M" x " " y "l 0 " (* l -1) " l -4 0 l 4 -10 l 4 10 l -4 0 z")})]
   (for [n (range 0 l 10)]
     ^{:key (str "yt" n)} [:path (merge path-defaults
                                        {:d (str "M" x " " (- y (* n scale)) "l -" (tick-length n) " 0")})])
   (for [n (range 0 l 50)]
     ^{:key (str "yl" n)} [:text (merge text-default {:x (- x 10) :y (- y (* n scale) -4) :text-anchor :end}) n])])

(defn histogram-view2
  "Renders a histogram for roundtrip times."
  [rtt-times x y w h]
    (let [mx (apply max rtt-times)
          mn (apply min rtt-times)
          bin-size (freedman-diaconis-rule rtt-times)
          bins (Math/round (/ (- mx mn) bin-size))
          binned-freq (frequencies (map (fn [n] (Math/floor (/ (- n mn) bin-size))) rtt-times))
          binned-freq-mx (apply max (map (fn [[_ f]] f) binned-freq))
          x-axis-l (max (+ (* (Math/ceil (/ mx 50)) 50) 20) 130)
          ;y-axis-l (min (max (+ (* (Math/ceil (/ max-freq 10)) 10) 20) 70) 110)
          y-axis-l 100
          ;y-axis-l (+ (* (Math/ceil (/ binned-freq-mx 10)) 10) 20)
          y-scale (/ (- h 20) binned-freq-mx)]
      [:g
       [:rect {:x x :y (- y h) :stroke :red :width w :height h :fill "rgba(0,0,255,0.1)"}]
       [:rect {:x (- x 45) :y (- y h) :stroke :red :width (+ w 45) :height (+ 40 h) :fill "rgba(0,255,0,0.1)"}]
       (when (pos? (count rtt-times))
         (for [[v f] binned-freq]
         ^{:key (str "bf" v f)} [:rect {:x (+ x (* mn 2) (* v 2 bin-size)) :y (- y (* f y-scale)) :fill "steelblue"
                                        :stroke "black" :width (* bin-size 2) :height (* f y-scale)}]))
       [x-axis x y x-axis-l 2]
       [:text (merge x-axis-label text-bold {:x (+ x x-axis-l -10) :y (+ y 38)}) "Roundtrip t/ms"]
       [:text (let [x-coord (- x 35) y-coord (- y (/ h 3)) rotate (str "rotate(270 " x-coord " " y-coord ")")]
                (merge x-axis-label text-bold {:x x-coord :y y-coord :transform rotate})) "Frequencies"]
       [y-axis2 x y h y-scale]
       ]))

(defn trailing-circles
  "Displays two transparent circles where one is drawn directly on the client and the other is drawn after a roundtrip.
  This makes it easier to experience any delays."
  [state]
  (let [pos (:pos state)
        from-server (:from-server state)]
    [:g
     [:circle (merge circle-defaults {:cx (:x pos) :cy (:y pos)})]
     [:circle (merge circle-defaults {:cx (:x from-server) :cy (:y from-server) :fill "rgba(0,0,255,0.1)"})]]))

(defn text-view
  "Renders SVG with an area in which mouse moves are detected. They are then sent to the server and the round-trip
  time is measured."
  [state pos mean mn mx latency]
  [:g
   [:text (merge text-bold {:x 130 :y 20}) "Mouse Moves Processed:"]
   [:text (merge text-default {:x 283 :y 20}) (:count state)]
   [:text (merge text-bold {:x 130 :y 40}) "Current Position:"]
   (when pos [:text (merge text-default {:x 237 :y 40}) (str "x: " (:x pos) " y: " (:y pos))])
   [:text (merge text-bold {:x 130 :y 60}) "Latency (ms):"]
   (when latency
     [:text (merge text-default {:x 215 :y 60}) (str mean " mean / " mn " min / " mx " max / " latency " last")])])

(defn mouse-view
  "Renders SVG with an area in which mouse moves are detected. They are then sent to the server and the round-trip
  time is measured."
  [app put-fn]
  (let [state @app
        pos (:pos state)
        chart-w (*  (.. js/window -innerWidth) 0.92)
        chart-h 350
        latency (:latency (:from-server state))
        rtt-times (:rtt-times state)
        mx (apply max rtt-times)
        mn (apply min rtt-times)
        mean (/ (apply + rtt-times) (count rtt-times))]
    [:svg {:width chart-w :height chart-h :style {:background-color :white}
           :on-mouse-move (mouse-move-ev-handler app put-fn (r/current-component))}
     [text-view state pos (.toFixed mean 0) mn mx latency]
     [trailing-circles state]
     [histogram-view rtt-times 90 280 300 200]
     [histogram-view2 rtt-times 500 280 300 150]]))

(defn mouse-pos-from-server!
  "Handler function for mouse position messages received from server."
  [app pos]
  (let [latency (- (now) (:timestamp pos))
        with-ts (assoc pos :latency latency)]
    (swap! app assoc :from-server with-ts)
    (swap! app update-in [:count] inc)
    (swap! app update-in [:rtt-times] conj latency)))

(defn in-handler
  "Handle incoming messages: process / add to application state."
  [app put-fn msg]
  (match msg
         [:cmd/mouse-pos-proc pos] (mouse-pos-from-server! app pos)
         :else (prn "unknown msg in data-loop" msg)))

(defn mk-state
  "Return clean initial component state atom."
  [put-fn]
  (let [app (atom {:count 0 :rtt-times []})]
    (r/render-component [mouse-view app put-fn] (by-id "mouse"))
    app))

(defn component
  [cmp-id]
  (comp/make-component cmp-id mk-state in-handler nil))