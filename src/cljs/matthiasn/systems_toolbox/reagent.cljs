(ns matthiasn.systems-toolbox.reagent
  (:require [reagent.core :as r :refer [create-class atom]]
            [matthiasn.systems-toolbox.component :as comp]
            [matthiasn.systems-toolbox.helpers :refer [by-id]]))

(defn init
  "Return clean initial component state atom."
  [reagent-cmp-map dom-id init-state init-fn put-fn]
  (let [initial-state (or init-state {})
        local (atom initial-state)
        observed (atom {})
        cmd (fn ([& r] (fn [e] (.stopPropagation e) (put-fn (vec r)))))
        reagent-cmp (create-class reagent-cmp-map)
        view-cmp-map {:observed observed
                      :local    local
                      :put-fn   put-fn
                      :cmd      cmd}]
    (r/render-component [reagent-cmp view-cmp-map] (by-id dom-id))
    (when init-fn (init-fn view-cmp-map))
    {:state {:local local
             :observed observed
             :initial-state initial-state}}))

(defn default-state-pub-handler
  "Default handler function, can be replaced by a more application-specific handler function, for example
  for resetting local component state when user is not logged in."
  [{:keys [cmp-state msg-payload]}]
  (reset! (:observed cmp-state) msg-payload))

(defn cmp-map
  "Creates a component map for a UI component using Reagent. This map can then be used by the comp/make-component
  function to initialize a component. Typically, this would be done by the switchboard."
  {:added "0.3.1"}
  [{:keys [cmp-id view-fn lifecycle-callbacks dom-id initial-state init-fn cfg handler-map state-pub-handler]}]
  (let [reagent-cmp-map (merge lifecycle-callbacks {:reagent-render view-fn})
        mk-state (partial init reagent-cmp-map dom-id initial-state init-fn)]
    {:cmp-id            cmp-id
     :state-fn          mk-state
     :handler-map       handler-map
     :state-pub-handler (or state-pub-handler default-state-pub-handler)
     :opts              (merge cfg {:watch :local})}))

(defn component
  [cmp]
  {:deprecated "0.3.1"}
  (comp/make-component (cmp-map cmp)))
