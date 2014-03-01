(ns ring.middleware.jsonp
  (:import (java.io ByteArrayInputStream
                    File
                    FileInputStream
                    InputStream
                    SequenceInputStream)
           (java.nio.charset Charset)
           (clojure.lang SeqEnumeration))
  (:use [clojure.string     :only (lower-case)]
        [ring.util.response :as response]))

(defn- get-param [request param]
  (or (get-in request [:params (keyword param)])
      (get-in request [:params (name    param)])))

(defn- re-matches? [^java.util.regex.Pattern re s]
  (-> (.matcher re s)
      (.matches)))

(defn- get-charset [content-type]
  (if-let [charset (re-find #"(?<=charset=)[^;]*" content-type)]
    (Charset/forName charset)
    (Charset/defaultCharset)))

(defn- get-content-type [response]
  (get-in response [:headers "Content-Type"] ""))

(defn- json-content-type? [content-type]
  (re-matches? #"application/(.*\+)?json(;.*)?" content-type))

(defn- string->stream [^String s ^Charset charset]
  (ByteArrayInputStream. (.getBytes s charset)))

(defn- concat-streams [xs]
  (->> xs seq SeqEnumeration. SequenceInputStream.))

(defn- body->stream [body charset]
  (cond (seq? body) (concat-streams
                     (for [x body] (string->stream (str x) charset)))
        (instance? File body) (FileInputStream. ^File body)
        (instance? InputStream body) body
        (string? body) (string->stream body charset)
        (nil? body) (string->stream "" charset)
        :else (throw (Exception. (str "Don't know how to convert "
                                      (type body)
                                      " to an InputStream!")))))

(defn- add-padding-to-json [callback content-type response]
  (let [charset (get-charset content-type)]
    (-> response
        (response/content-type (str "application/javascript; charset=" (lower-case charset)))
        (update-in [:body]
                   #(concat-streams
                     [(string->stream (str callback "(") charset)
                      (body->stream % charset)
                      (string->stream ");" charset)])))))

(defn wrap-json-with-padding [handler]
  (fn [request]
    (let [callback (get-param request :callback)
          response (handler request)
          content-type (get-content-type response)]
      (if (and callback (json-content-type? content-type))
          (add-padding-to-json callback content-type response)
          response))))
