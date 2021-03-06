package main

import (
	"flag"
	"net"
	"runtime"

	service "grpc"

	"github.com/smallnest/rpcx/log"
	"golang.org/x/net/context"
	"google.golang.org/grpc"
)

type Hello struct{}

func (t *Hello) Say(ctx context.Context, args *service.BenchmarkMessage) (reply *service.BenchmarkMessage, err error) {
	s := "OK"
	var i int32 = 100
	args.Field1 = s
	args.Field2 = i
	return args, nil
}

var host = flag.String("s", "127.0.0.1:8972", "listened ip and port")

func main() {
	runtime.GOMAXPROCS(12)
	flag.Parse()

	lis, err := net.Listen("tcp", *host)
	if err != nil {
		log.Fatalf("failed to listen: %v", err)
	}
	s := grpc.NewServer(grpc.InitialWindowSize(65535*100), grpc.InitialConnWindowSize(65535*1000))

	service.RegisterHelloServer(s, &Hello{})
	s.Serve(lis)
}
