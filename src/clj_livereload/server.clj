(ns clj-livereload.server
  (:require [clj-livereload.core :refer :all]))

(def server (atom nil))

(defn start!
  ([] (start! nil))
  ([opts]
   (reset! server (start opts))
   :started))

(defn stop! []
  (stop @server)
  (reset! server nil)
  :stopped)

(defn send-reload-msg! [path]
  (if-let [state (:state @server)]
    (send-reload-msg state path)))
