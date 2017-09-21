(ns cf-async-service-broker.handler
  (:require [compojure.core :refer [GET PUT DELETE routes defroutes]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json-response :refer [wrap-json-response]]
            [clojure.tools.logging :as log]
            [ring.util.response :refer [response]]

            [clojure.data.json :as json]
            [cf-lib.core :refer [cf-extract-guid cf-extract-entity-field
                                 cf-extract-name
                                 cf-org cf-space]]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            ))

(def instance-provision-time (atom {}))
(def SECS 1000)
(def PROVISIONING-DELAY-SECS 2)

(defn time-remaining-millis [guid]
  (let [curr-time-millis (System/currentTimeMillis)
        provision-time-millis (get @instance-provision-time guid)]
    (when-not (nil? provision-time-millis)
      (max (- provision-time-millis curr-time-millis) 0))))

(defn completed? [guid]
  (<= (time-remaining-millis guid) 0))

(defn logger-middleware [handler]
  (fn [req]
    ;; (log/infof "request: %s" req)
    (log/infof "request: %s %s %s"
               (-> req :request-method)
               (-> req :uri)
               (-> req :query-string))
    (let [resp (handler req)]
      (log/infof "resp is %s" resp)
      resp)))

(defroutes app-routes
  (GET "/" [] "OK")

  (GET "/v2/catalog" []
       (-> {"services"
            [{"name" "dummy-async-service-local"
              "id" "7853521e-9c7f-11e7-bcc2-525400c583ad-local"
              "description" "a dummy service supporting async provisioining"
              "bindable" true
              "tags" ["poc"]
              "plans" [{"name" "free"
                        "id" "8fbe000c-9c7f-11e7-a888-525400c583ad-local"
                        "description" "free"}]
              "requires" ["route_forwarding"]}]}
           response
           ))

  (PUT "/v2/service_instances/:guid" [guid]
       (fn [req]
         (let [accepts-incomplete?
               (-> req (ring.middleware.params/assoc-query-params "utf8")
                   :query-params (get "accepts_incomplete")
                   Boolean/valueOf)
               payload (-> req :body slurp json/read-str)
               provision-time-millis (* (or (-> payload (get "parameters")
                                                (get "secs")
                                                Integer/parseInt)
                                            PROVISIONING-DELAY-SECS)
                                        SECS)]
           (if accepts-incomplete?
             (let [provision-time (+ (System/currentTimeMillis)
                                     provision-time-millis)]
               (swap! instance-provision-time #(assoc %1 guid provision-time))
               (-> {} response
                   (ring.util.response/status 202)))
             (-> {"error" "only async provisioning supported"}
                 response
                 (ring.util.response/status 422))))))

  (GET "/v2/service_instances/:guid/last_operation" [guid]
       (let [time-remaining-millis (time-remaining-millis guid)]
         (if (nil? time-remaining-millis)
           (ring.util.response/not-found
            (format "no such instance: %s" guid))
           (let [completed (= 0 time-remaining-millis)]
             (->
              {"state" (if (completed? guid)
                         "succeeded"
                         "in progress")
               "description" (format "%d secs remaining"
                                     (-> time-remaining-millis
                                         (/ SECS)
                                         int))
               } response)))))

  (DELETE "/v2/service_instances/:guid" [guid]
          (swap! instance-provision-time
                 #(dissoc %1 guid))
          (-> {} response))

  (PUT "/v2/service_instances/:instance-guid/service_bindings/:binding-guid"
       [instance-guid binding-guid]
       (if (completed? instance-guid)
         {:status 201
          :body {"credentials"
                 {"provision-completed-time-millis"
                  (get @instance-provision-time
                       instance-guid)}}}
         (-> {"error" "async provisioning still in progress"}
             response
             (ring.util.response/status 422))))

  (DELETE "/v2/service_instances/:instance-id/service_bindings/:binding-guid"
          [instance-guid binding-guid]
          (-> {} response))

  (route/not-found "Not Found"))

(def app
  (->
   app-routes
   wrap-json-response
   logger-middleware
   ;; ring.middleware.params/wrap-params
   ))

(defn start-dev-server [app port]
  (if (and (resolve `test-server)
           (bound? (resolve `test-server))
           (var-get (resolve `test-server)))
    (.stop (var-get (resolve `test-server))))

  (def test-server (->
                                        ;(app-for-cf-login cf-login)
                    app
                    ;; ring.middleware.stacktrace/wrap-stacktrace
                    (ring.adapter.jetty/run-jetty
                     {:port (or port 1223) :join? false}))))

;; (start-dev-server app 3000)
;; (ring.adapter.jetty/run-jetty app {:port 1225 :join? false})
