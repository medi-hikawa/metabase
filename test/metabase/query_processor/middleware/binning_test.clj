(ns metabase.query-processor.middleware.binning-test
  (:require [expectations :refer [expect]]
            [metabase.query-processor.middleware
             [binning :refer :all]
             [expand :as ql]]
            [metabase.test.util :as tu]))

(tu/resolve-private-vars metabase.query-processor.middleware.binning filter->field-map extract-bounds ceil-to floor-to order-of-magnitude nicer-bin-width nicer-bounds nicer-breakout)

(expect
  {}
  (filter->field-map (ql/and
                      (ql/= (ql/field-id 1) 10)
                      (ql/= (ql/field-id 2) 10))))

(expect
  {1 [(ql/< (ql/field-id 1) 10) (ql/> (ql/field-id 1) 1)]
   2 [(ql/> (ql/field-id 2) 20) (ql/< (ql/field-id 2) 10)]
   3 [(ql/between (ql/field-id 3) 5 10)]}
  (filter->field-map (ql/and
                      (ql/< (ql/field-id 1) 10)
                      (ql/> (ql/field-id 1) 1)
                      (ql/> (ql/field-id 2) 20)
                      (ql/< (ql/field-id 2) 10)
                      (ql/between (ql/field-id 3) 5 10))))

(expect
  [[1.0 1.0 1.0]
   [1.0 2.0 2.0]
   [15.0 15.0 30.0]]
  [(mapv (partial floor-to 1.0) [1 1.1 1.8])
   (mapv (partial ceil-to 1.0) [1 1.1 1.8])
   (mapv (partial ceil-to 15.0) [1.0 15.0 16.0])])


(expect
  [-2.0 -1.0 0.0 1.0 2.0 3.0]
  (map order-of-magnitude [0.01 0.5 4 12 444 1023]))

(expect
  [20.0 2000.0]
  [(nicer-bin-width 27 135 8)
   (nicer-bin-width -0.0002 10000.34 8)])

(expect
  [1 10]
  (extract-bounds {:field-id 1 :min-value 100 :max-value 1000}
                  {1 [(ql/> (ql/field-id 1) 1) (ql/< (ql/field-id 1) 10)]}))

(expect
  [1 10]
  (extract-bounds {:field-id 1 :min-value 100 :max-value 1000}
                  {1 [(ql/between (ql/field-id 1) 1 10)]}))

(expect
  [100 1000]
  (extract-bounds {:field-id 1 :min-value 100 :max-value 1000}
                  {}))

(expect
  [500 1000]
  (extract-bounds {:field-id 1 :min-value 100 :max-value 1000}
                  {1 [(ql/> (ql/field-id 1) 500)]}))

(expect
  [100 500]
  (extract-bounds {:field-id 1 :min-value 100 :max-value 1000}
                  {1 [(ql/< (ql/field-id 1) 500)]}))

(expect
  [600 700]
  (extract-bounds {:field-id 1 :min-value 100 :max-value 1000}
                  {1 [(ql/> (ql/field-id 1) 200)
                      (ql/< (ql/field-id 1) 800)
                      (ql/between (ql/field-id 1) 600 700)]}))

(expect
  [[0.0 1000.0 125.0 8]
   [200.0 1600.0 200.0 8]
   [0.0 1200.0 200.0 8]
   [0.0 1005.0 15.0 67]]
  [((juxt :min-value :max-value :bin-width :num-bins)
         (nicer-breakout {:field-id 1 :min-value 100 :max-value 1000
                          :strategy :num-bins :num-bins 8}))
   ((juxt :min-value :max-value :bin-width :num-bins)
         (nicer-breakout {:field-id 1 :min-value 200 :max-value 1600
                          :strategy :num-bins :num-bins 8}))
   ((juxt :min-value :max-value :bin-width :num-bins)
         (nicer-breakout {:field-id 1 :min-value 9 :max-value 1002
                          :strategy :num-bins :num-bins 8}))
   ((juxt :min-value :max-value :bin-width :num-bins)
         (nicer-breakout {:field-id 1 :min-value 9 :max-value 1002
                          :strategy :bin-width :bin-width 15.0}))])