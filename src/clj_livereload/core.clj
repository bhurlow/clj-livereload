(ns clj-livereload.core
  (:require [compojure.core :refer [context routes GET OPTIONS POST]]
            [org.httpkit.server :refer [run-server with-channel on-close on-receive send! open?]]
            [ring.middleware.reload :refer :all]
            [ring.util.response :refer :all]
            [cheshire.core :as json]
            [hawk.core :as hawk]))

(defn- hello-message []
  {:command "hello"
   :protocols ["http://livereload.com/protocols/official-7"
               "http://livereload.com/protocols/official-8"
               "http://livereload.com/protocols/official-9"
               "http://livereload.com/protocols/2.x-origin-version-negotiation"
               "http://livereload.com/protocols/2.x-remote-control"]
   :serverName "clj-livereload"})

(defn css-file? [path]
  (.endsWith path ".css"))

(defn send-reload-msg [state path]
  (println "trigggered reload for path" path)
  (doseq [channel (:reload-channels @state)]
    (if (open? channel)
      (send! channel
             (json/generate-string
               {:command "reload"
                :path (str "/" path)
                :liveCSS true}))
      (swap! state update-in [:reload-channels disj channel]))))

(defn- watch-params [state paths]
  [{:paths [paths]
    :handler (fn [ctx e]
               (println "detected file change")
               (send-reload-msg state (.getName (:file e))))}])

(defn- watch-directory [state dir]
  (println "starting to watch dir..")
  (hawk/watch! (watch-params state dir)))

(defn- handle-livereload [state req]
  (with-channel req channel
    (swap! state update-in [:reload-channels] conj channel)
    (on-receive
      channel
      (fn [data]
        (let [parsed (json/decode data true)]
          (println parsed)
          (case (:command parsed)
            "hello" (send! channel (json/generate-string (hello-message)))
            nil))))
    (on-close
      channel
      (fn [_]
        (swap! state update-in [:reload-channels] disj channel)))))

(defn- handler [state]
  (routes
    (GET "/livereload.js" req
      (-> (resource-response "META-INF/resources/webjars/livereload-js/2.2.2/dist/livereload.js" {:root ""})
          (content-type "application/javascript")))
    (GET "/livereload" req
      (handle-livereload state req))))

(defn create-state
  "Creates the state holding open websocket connections."
  []
  (atom {:reload-channels #{}}))

(defn start
  "Starts the http-server and (by default) watch service.
   Returns a map containg the server state. It can be passed to
   stop function to stop the server.

   Options:
   - :port     Default 35729.
   - :watch    Default true. To disable directory watching, set false."
  [{:keys [dir port watch]
    :or {watch true}}]
  (let [state (create-state)]
    (println "starting LiveReload server")
    {:state state
     :http-kit (run-server (handler state) {:port (or port 35729)})
     :watch (if watch
              (watch-directory state dir))}))

(defn stop
  "Stop the http-server and watch service."
  [server]
  (if-let [watch (:watch server)]
    (hawk/stop! watch))
  (if-let [http-kit (:http-kit server)]
    (http-kit :timeout 100))
  nil)
