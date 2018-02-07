### GTFS streaming data

Run on OpenShift

```
git clone git@github.com:eformat/onebusaway-gtfs-realtime-visualizer.git
cd onebusaway-gtfs-realtime-visualizer

oc new-project gtfs --description="Transit Data App" --display-name="Transit Data App"
oc new-build --binary --name gtfs
oc start-build gtfs --from-dir=. --follow
oc new-app --image-stream=gtfs
oc expose svc/gtfs --port=8080 --name=gtfs

-- brisbane
oc set env dc gtfs GTFS_SOURCE="--vehiclePositionsUrl=https://gtfsrt.api.translink.com.au/Feed/SEQ"

-- sydney buses
oc set env dc gtfs GTFS_SOURCE="--vehiclePositionsUrl=https://api.transport.nsw.gov.au/v1/gtfs/vehiclepos/buses --apiKey=<your api key>"

-- sydney ferries
oc set env dc gtfs GTFS_SOURCE="--vehiclePositionsUrl=https://api.transport.nsw.gov.au/v1/gtfs/vehiclepos/ferries --apiKey=<your api key>"

-- Sydney Trains
oc set env dc gtfs GTFS_SOURCE="--vehiclePositionsUrl=https://api.transport.nsw.gov.au/v1/gtfs/vehiclepos/sydneytrains --apiKey=<your api key>"
```
