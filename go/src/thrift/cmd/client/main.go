package main

import (
	"context"
	"flag"
	"fmt"
	"os"
	"reflect"
	"sync"
	"sync/atomic"
	model "thrift"
	"time"

	"git.apache.org/thrift.git/lib/go/thrift"
	"github.com/montanaflynn/stats"
	"github.com/smallnest/rpcx/log"
)

func Usage() {
	fmt.Fprint(os.Stderr, "Usage of ", os.Args[0], ":\n")
	flag.PrintDefaults()
	fmt.Fprint(os.Stderr, "\n")
}

func main() {
	flag.Usage = Usage
	protocol := flag.String("P", "binary", "Specify the protocol (binary, compact, json, simplejson)")
	framed := flag.Bool("framed", true, "Use framed transport")
	buffered := flag.Bool("buffered", true, "Use buffered transport")
	addr := flag.String("addr", "127.0.0.1:9090", "Address to listen to")
	concurrency := flag.Int("c", 1, "concurrency")
	total := flag.Int("n", 10000, "total requests for all clients")

	flag.Parse()
	n := *concurrency
	m := *total / n

	var protocolFactory thrift.TProtocolFactory
	switch *protocol {
	case "compact":
		protocolFactory = thrift.NewTCompactProtocolFactory()
	case "simplejson":
		protocolFactory = thrift.NewTSimpleJSONProtocolFactory()
	case "json":
		protocolFactory = thrift.NewTJSONProtocolFactory()
	case "binary", "":
		protocolFactory = thrift.NewTBinaryProtocolFactoryDefault()
	default:
		fmt.Fprint(os.Stderr, "Invalid protocol specified", protocol, "\n")
		Usage()
		os.Exit(1)
	}
	var transportFactory thrift.TTransportFactory
	if *buffered {
		transportFactory = thrift.NewTBufferedTransportFactory(8192)
	} else {
		transportFactory = thrift.NewTTransportFactory()
	}

	if *framed {
		transportFactory = thrift.NewTFramedTransportFactory(transportFactory)
	}

	msg := prepareArgs()
	var wg sync.WaitGroup
	wg.Add(n)
	fmt.Println(msg)
	var trans uint64
	var transOK uint64
	d := make([][]int64, n, n)
	totalT := time.Now().UnixNano()

	for i := 0; i < n; i++ {
		dt := make([]int64, 0, m)
		d = append(d, dt)

		go func(cNum int) {
			defer wg.Done()
			var err error
			var transport thrift.TTransport

			transport, err = thrift.NewTSocket(*addr)
			if err != nil {
				log.Warnf("get client failed,err:%v\n", err)
				return
			}
			transport, err = transportFactory.GetTransport(transport)
			if err != nil {
				log.Warnf("get client failed,err:%v\n", err)
				return
			}
			if err = transport.Open(); err != nil {
				log.Warnf("get client failed,err:%v\n", err)
				return
			}
			defer transport.Close()

			iprot := protocolFactory.GetProtocol(transport)
			oprot := protocolFactory.GetProtocol(transport)
			c := model.NewGreeterClient(thrift.NewTStandardClient(iprot, oprot))

			//warmup
			for j := 0; j < 1; j++ {
				c.Say(context.TODO(), msg)
			}

			for j := 0; j < m; j++ {
				t := time.Now().UnixNano()
				reply, err := c.Say(context.TODO(), msg)
				t = time.Now().UnixNano() - t

				d[i] = append(d[i], t)

				if err == nil && reply.Field1 == "OK" {
					atomic.AddUint64(&transOK, 1)
				}

				atomic.AddUint64(&trans, 1)
			}

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
