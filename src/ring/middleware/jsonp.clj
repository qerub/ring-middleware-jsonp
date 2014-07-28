(ns ring.middleware.jsonp
  (:import (java.io ByteArrayInputStream
                    File
                    FileInputStream
                    InputStream
                    SequenceInputStream)
           (java.nio.charset Charset)
           (clojure.lang Sequential
                         SeqEnumeration))
  (:use [clojure.string :only (lower-case)]))

(defn ^:private get-param [request param]
  (or (get-in request [:params (keyword param)])
      (get-in request [:params (name    param)])))

(defn ^:private re-matches? [^java.util.regex.Pattern re s]
  (-> (.matcher re s)
      (.matches)))

(defn ^:private get-charset [content-type]
  (if-let [charset (re-find #"(?<=charset=)[^;]*" content-type)]
    (Charset/forName charset)
    (Charset/defaultCharset)))

(defn ^:private get-content-type [response]
  (get-in response [:headers "Content-Type"] ""))

(defn ^:private json-content-type? [content-type]
  (re-matches? #"application/(.*\+)?json(;.*)?" content-type))

(defn ^:private valid-callback? [s]
  (re-matches? #"[a-zA-Z0-9_.]+" s))

(def ^:private response-for-invalid-callback
  {:status 422, :headers {"Content-Type" "text/plain"}, :body "Invalid callback parameter"})

(defprotocol Streamable
  (->stream [x cs]))

(extend-protocol Streamable
  ;; TODO: Add CollReduce when we depend on Clojure >= 1.4
  String      (->stream [x cs] (ByteArrayInputStream. (.getBytes x ^Charset cs)))
  File        (->stream [x cs] (FileInputStream. x))
  InputStream (->stream [x cs] x)
  Sequential  (->stream [x cs] (->> x (map #(->stream % cs)) SeqEnumeration. SequenceInputStream.))
  nil         (->stream [x cs] (->stream "" cs)))

(defn ^:private add-padding-to-json [callback content-type response]
  (let [cs (get-charset content-type)]
    (-> response
        (assoc-in [:headers "Content-Type"] (str "application/javascript; charset=" (lower-case cs)))
        (update-in [:body] #(->stream [callback "(" % ");"] cs)))))

(defn wrap-json-with-padding [handler]
  (fn [request]
    (let [callback (get-param request :callback)
          response (handler request)
          content-type (get-content-type response)]
      (if (and callback (json-content-type? content-type))
          (if (valid-callback? callback)
            (add-padding-to-json callback content-type response)
            response-for-invalid-callback)
          response))))
