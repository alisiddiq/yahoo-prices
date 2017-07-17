(ns yahoo-prices.prices-and-returns
  (:require [net.cgrand.enlive-html :as html]
            [clj-http.cookies :as cookies]
            [clojure.string :as str]
            [yahoo-prices.utils :as utils]
            [spyscope.core :refer :all]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clj-time.format :as f]
            [clojure.data.csv :as csv]))


(defn- get-cookies
  [code]
  (let [url (str "https://finance.yahoo.com/quote/" code "/history?p=" code)
        cookie-jar (cookies/cookie-store)
        http-request (:body (utils/http-request :get url :cookie-store cookie-jar))
        crumb (last (str/split (re-find #"\"CrumbStore\":\{\"crumb\":\".{11}" http-request) #"\""))
        cookie-val (:value (get (clj-http.cookies/get-cookies cookie-jar) "B"))
        ]
    {:cookie cookie-val :crumb crumb}
    )
  )

(defn get-yahoo-prices
  "Get prices from Yahoo, start and end dates should be in the format yyyy-MM-dd"
  [code start end & {:keys [interval retry-attempts sleep-time]}]
  (let [interval (or interval "1d")
        retry-attempts (or retry-attempts 10)
        sleep-time (or sleep-time 10)
        cookies (get-cookies code)
        start-date (quot (tc/to-long (f/parse (f/formatters :date) start)) 1000)
        end-date (quot (tc/to-long (f/parse (f/formatters :date) end)) 1000)
        url (str "https://query1.finance.yahoo.com/v7/finance/download/" code "?period1=" start-date "&period2=" end-date "&interval=" interval "&events=history&crumb=" (:crumb cookies))
        ]
    (if-not (zero? retry-attempts)
      (try (csv/read-csv (:body (utils/http-request
                                  :get url
                                  :headers {"Cookie" (str "B=" (:cookie cookies))}
                                  )))
           (catch Exception e
             (do (println (str "Exception found: " (.getMessage e) ", retrying after " sleep-time " seconds..."))
                 (Thread/sleep (* sleep-time 1000))
                 (get-yahoo-prices code start end :interval interval :retry-attempts (dec retry-attempts) :sleep-time sleep-time)
                 )))
      (println "Failed after re-attempts :( exiting...")
      )
    ))

(defn get-returns
  "Get the daily/weekly/monthly returns. Defaults to daily, interval values could be '1d', '1wk', '1mo'"
  [code since end {:keys [interval]}]
  (let [interval (or interval "1d")
        ;now-str (f/unparse (f/formatter "yyyy-MM-dd") (t/now))
        past-price-data (utils/tc #(rest (get-yahoo-prices code since end :interval interval)))
        csv-data (if-not (nil? past-price-data)
                   (mapv #(read-string (nth % 4)) (vec past-price-data))
                   nil)
        csv-data-new (vec (rest csv-data))
        csv-data-old (vec (butlast csv-data))
        perc-change (mapv utils/perc-change csv-data-new csv-data-old)
        map-fn (fn [date-entry change] [(keyword date-entry) change])
        out-dates (mapv #(first %) (rest past-price-data))
        perc-change-date (into (sorted-map) (mapv map-fn out-dates perc-change))
        ]
    perc-change-date
    ))

;

(defn get-daily-returns
  [code since]
  (let [now-str (f/unparse (f/formatter "yyyy-MM-dd") (t/now))]
    (get-returns code since now-str {:interval "1d"})
    ))

(defn get-weekly-returns
  [code since]
  (let [now-str (f/unparse (f/formatter "yyyy-MM-dd") (t/now))]
    (get-returns code since now-str {:interval "1wk"})
    ))


(defn get-monthly-returns
  [code since]
  (let [now-str (f/unparse (f/formatter "yyyy-MM-dd") (t/now))]
    (get-returns code since now-str {:interval "1mo"})
    ))

;(defn get-last-sunday
  ;  [& [cust-date]]
  ;  (let [date (or cust-date (t/now))
  ;        day #spy/p (t/day-of-week date)
  ;        adjusted-day (if (>= day 6) 0 day)
  ;        last-sunday (t/minus date (t/days adjusted-day))
  ;        ]
  ;    last-sunday
  ;    ))

;(defn get-weekly-returns
;  [code since]
;  (let [last-sunday-str (f/unparse (f/formatter "yyyy-MM-dd") (get-last-sunday))]
;    (get-returns code since last-sunday-str {:interval "1wk"})
;    ))

