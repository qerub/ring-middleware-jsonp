# ring.middleware.jsonp

## Installation

Add the following to your `project.clj` `:dependencies`:

```clojure
[ring.middleware.jsonp "0.1.5"]
```

## Usage

Wrap your [Ring][] application with `wrap-json-with-padding` and
JSON resources will automatically support JSONP.

Example:

```clojure
(use 'ring.middleware.jsonp 'ring.util.response)

(def app
  (-> (response "{\"result\": 42}")
      (content-type "application/json")
      (constantly)
      (wrap-json-with-padding))) ; <--

(:body (app {}))
; => "{\"result\": 42}"

(:body (app {:params {:callback "f"}}))
; => "f({\"result\": 42});"
```

### Compojure

If you're using Compojure, make sure `wrap-json-with-padding` is placed above your site handler (if you have one).

Example:

```clojure
(def app
  (-> app-routes
      (wrap-json-with-padding) ; <--
      (handler/site)
      ...))
```

## License

Copyright (C) 2012–2013
[Christoffer Sawicki](mailto:christoffer.sawicki@gmail.com),
Gary Fredericks,
Markus Hjort

Distributed under the Eclipse Public License, the same as Clojure.

[Ring]: https://github.com/ring-clojure/ring
