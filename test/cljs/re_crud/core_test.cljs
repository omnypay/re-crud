(ns re-crud.core-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests async]]
            [re-frame.registrar :as registrar]
            [re-frame.core :as re-frame]
            [re-crud.core :as crud]
            [re-crud.components :as comp]
            [re-crud.components.utils :as u]))

(defonce things-to-test
  (atom #{:create-components
          :fetch
          :perform}))

(defn init []
  (crud/init
   {"test-service" {:service-url "http://localhost:8000"
                    :swagger-url "http://localhost:8000/swagger.json"
                    :dispatch-on-ready [:on-ready]}}))

(defn create-components [{:keys [db]} _]
  (let [component (comp/update {:id      :user.update
                                :fetch   {:operation-id "getUser"
                                          :after (u/create-fx (fn [_]
                                                                (re-frame/dispatch [:invoke-perform])
                                                                (re-frame/dispatch [:assert! :fetch])))}
                                :form    {:operation-id "updateUser"}
                                :perform {:operation-id "updateUser"
                                          :after (u/create-fx (fn [response]
                                                                (re-frame/dispatch [:assert! :perform response])))}
                                :config  {:service-name "test-service"}})]
    {:dispatch-n [[:crud-load-component component {:fetch {:user-id 1}
                                                   :form  {:user-id 1}}]
                  [:assert! :create-components]]}))

(defn invoke-perform [{:keys [db]} _]
  (let [data (-> (get-in db (u/resource-path :user.update))
                 (assoc :user-id 1))]
    {:dispatch-n [[:crud-components :user.update :ui :user-input :email "updatedemail@example.com"]
                  [:crud-components :user.update :ui :user-input :user-id 1]
                  [:crud-perform-user.update]]}))

(defn create-components-assertions []
  (is (some? (registrar/get-handler :event :crud-fetch-user.update)))
  (is (some? (registrar/get-handler :event :crud-perform-user.update))))

(defn update-assertions [data]
  (is (= (set (keys data))
         #{:id :first-name :last-name :email :url}))
  (is (= "updatedemail@example.com"
         (:email data))))

(defn assert! [{:keys [db]} [_ thing-to-test data]]
  (swap! things-to-test disj thing-to-test)
  (case thing-to-test
    :create-components (create-components-assertions)
    :fetch             (is (= (set (keys (get-in db (u/resource-path :user.update))))
                              #{:id :first-name :last-name :email :url}))
    :perform (update-assertions data))
  (if (empty? @things-to-test)
    {:done []}
    {}))

(defn register-events [done]
  (re-frame/reg-event-fx :assert! assert!)
  (re-frame/reg-event-fx :invoke-perform invoke-perform)
  (re-frame/reg-event-fx :create-components create-components)
  (re-frame/reg-event-fx :on-ready (fn [_ _] {:dispatch [:create-components]}))
  (re-frame/reg-fx :done (fn [_] (done))))

(deftest event-handlers
  (async done
         (register-events done)
         (init)))
