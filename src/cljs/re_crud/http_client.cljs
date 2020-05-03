(ns re-crud.http-client
  (:require [re-frame.core :refer [dispatch]]
            [ajax.core :refer [POST GET] :as ajax]
            [cljs.reader :as reader]
            [clojure.string :as s]))

(defn log [& args]
  (.log js/console :info args))

(defn make-url [service-url url request-params]
  (->> (reduce (fn [u [k v]] (s/replace u (str "{"(name k)"}") v))
               url
               request-params)
       (str service-url)))

(defn parse-json-string [string]
  (js->clj (.parse js/JSON string)))

(defn parse-response [response]
  (clojure.walk/keywordize-keys response))

(defn response-handler [log-id request-body response operation-id on-success]
  (let [parsed-response (parse-response response)]
    (when on-success
      (dispatch (conj on-success parsed-response)))))

(def actions
  {:get    ajax/GET
   :post   ajax/POST
   :put    ajax/PUT
   :patch  ajax/PATCH
   :delete ajax/DELETE})

(defn make-request [operation-id method url request-body & {:keys [on-success service-name on-failure]}]
  (let [log-id (random-uuid)
        action (get actions method)]
    (action url
            {:params request-body
             :headers {"x-re-crud-service" service-name
                       "Accept" "application/json"}
             :format :json
             :handler #(response-handler log-id request-body % operation-id  on-success)
             :error-handler (fn [{:keys [status response]}]
                              (when (some? on-failure)
                                (dispatch [on-failure status response]))
                              (dispatch [:crud-http-fail operation-id status response]))})))
