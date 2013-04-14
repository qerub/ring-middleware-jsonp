# ring.middleware.jsonp

## Installation

Add the following to your `project.clj` `:dependencies`:

```clojure
[ring.middleware.jsonp "0.1.2"]
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

## License

Copyright (C) 2012–2013
[Christoffer Sawicki](mailto:christoffer.sawicki@gmail.com) and
Gary Fredericks

Distributed under the Eclipse Public License, the same as Clojure.

[Ring]: https://github.com/ring-clojure/ring
