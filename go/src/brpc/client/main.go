package main

import (
	"context"
	"flag"
	"go-common/conf"
	"go-common/net/rpc"
	xtime "go-common/time"
	model "grpc"
	"reflect"
	"sync"
	"sync/atomic"
	"time"

	"github.com/montanaflynn/stats"
	"github.com/smallnest/rpcx/log"
)

var concurrency = flag.Int("c", 10, "concurrency")
var total = flag.Int("n", 100000, "total requests for all clients")

func main() {
	flag.Parse()
	n := *concurrency
	m := *total / n
	var clients []*rpc.Client2 = make([]*rpc.Client2, n)
	for i := 0; i < n; i++ {
		clients[i] = getClients()
	}
	msg := prepareArgs()
	var wg sync.WaitGroup
	wg.Add(n)

	var trans uint64
	var transOK uint64
	d := make([][]int64, n, n)
	totalT := time.Now().UnixNano()
	ctx := context.TODO()

	for i := 0; i < n; i++ {
		dt := make([]int64, 0, m)
		d = append(d, dt)

		go func(cNum int) {

			c := clients[cNum]
			//warmup
			for j := 0; j < 1; j++ {
				Say(ctx, c, msg)
			}

			for j := 0; j < m; j++ {
				t := time.Now().UnixNano()
				reply, err := Say(ctx, c, msg)
				t = time.Now().UnixNano() - t

				d[i] = append(d[i], t)

				if err == nil && reply.Field1 == "OK" {
					atomic.AddUint64(&transOK, 1)
				}

				atomic.AddUint64(&trans, 1)
			}
			wg.Done()

		}(i)

	}

	wg.Wait()
	totalT = time.Now().UnixNano() - totalT
	totalT = totalT / 1000000
	log.Infof("took %d ms for %d requests\n", totalT, n*m)

	totalD := make([]int64, 0, n*m)
	for _, k := range d {
		totalD = append(totalD, k...)
	}
	totalD2 := make([]float64, 0, n*m)
	for _, k := range totalD {
		totalD2 = append(totalD2, float64(k))
	}

	mean, _ := stats.Mean(totalD2)
	median, _ := stats.Median(totalD2)
	max, _ := stats.Max(totalD2)
	min, _ := stats.Min(totalD2)
	tp99, _ := stats.Percentile(totalD2, 99)
	tp999, _ := stats.Percentile(totalD2, 99.9)

	log.Infof("sent     requests    : %d\n", n*m)
	log.Infof("received requests    : %d\n", atomic.LoadUint64(&trans))
	log.Infof("received requests_OK : %d\n", atomic.LoadUint64(&transOK))
	log.Infof("throughput  (TPS)    : %d\n", int64(n*m)*1000/totalT)
	log.Infof("mean: %.f ns, median: %.f ns, max: %.f ns, min: %.f ns, p99: %.f ns %.f ns\n", mean, median, max, min, tp99, tp999)
	log.Infof("mean: %d ms, median: %d ms, max: %d ms, min: %d ms, p99: %d ms,p999:%d ms\n", int64(mean/1000000), int64(median/1000000), int64(max/1000000), int64(min/1000000), int64(tp99/1000000), int64(tp999/1000000))

}

func Say(ctx context.Context, c *rpc.Client2, msg *model.BenchmarkMessage) (res *model.BenchmarkMessage, err error) {
	res = new(model.BenchmarkMessage)
	err = c.Call(ctx, "RPC.Ping", msg, res)
	return
}

func getClients() *rpc.Client2 {
	c := conf.RPCClient{
		Proto:   "tcp",
		Addr:    "127.0.0.1:7111",
		Timeout: xtime.Duration(time.Second * 10),
		Breaker: &conf.Breaker{
			Window:  xtime.Duration(time.Second * 3),
			Sleep:   xtime.Duration(time.Millisecond * 100),
			Bucket:  10,
			Ratio:   0.5,
			Request: 100000,
		},
	}

	client := rpc.NewClient2(&conf.RPCClient2{
		Client: &c,
		Backup: &conf.RPCClients{&c},
		Zookeeper: &conf.Zookeeper{
			Root:    "/microservice/member-service/",
			Addrs:   []string{"127.0.0.1:2181"},
			Timeout: xtime.Duration(time.Second * 999999)},
		PullInterval: xtime.Duration(time.Second * 999999),
	})
	return client
}

func prepareArgs() *model.BenchmarkMessage {
	b := true
	var i int32 = 120000
	var i64 int64 = 98765432101234
	var s = "许多往事在眼前一幕一幕，变的那麼模糊"

	var args model.BenchmarkMessage

	v := reflect.ValueOf(&args).Elem()
	num := v.NumField()
	for k := 0; k < num; k++ {
		field := v.Field(k)
		if field.Type().Kind() == reflect.Ptr {
			switch v.Field(k).Type().Elem().Kind() {
			case reflect.Int, reflect.Int32:
				field.Set(reflect.ValueOf(&i))
			case reflect.Int64:
				field.Set(reflect.ValueOf(&i64))
			case reflect.Bool:
				field.Set(reflect.ValueOf(&b))
			case reflect.String:
				field.Set(reflect.ValueOf(&s))
			}
		} else {
			switch field.Kind() {
			case reflect.Int, reflect.Int32, reflect.Int64:
				field.SetInt(9876543)
			case reflect.Bool:
				field.SetBool(true)
			case reflect.String:
				field.SetString(s)
			}
		}

	}
	return &args
}
