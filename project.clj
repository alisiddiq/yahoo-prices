(defproject yahoo-prices "0.1"
  :description "Get daily/weekly/monthly returns and prices of equities from yahoo"
  :url "https://github.com/alisiddiq/yahoo_prices"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [enlive "1.1.1"]
                 [spyscope "0.1.5"]
                 [clj-http "2.0.1"]
                 [org.clojure/data.csv "0.1.3"]
                 ]
  :main ^:skip-aot yahoo-prices.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
