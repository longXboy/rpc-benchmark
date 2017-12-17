package main

import (
	"go-common/conf"
	"go-common/net/rpc"
	"go-common/net/rpc/context"
	model "grpc"
	"time"
)

// RPC rpc
type RPC struct {
}

// Ping check connection success.
func (r *RPC) Ping(c context.Context, arg *model.BenchmarkMessage, res *model.BenchmarkMessage) (err error) {
	*res = *arg
	res.Field1 = "OK"
	return
}

func main() {

	var servers []*conf.RPCServer
	s := &conf.RPCServer{
		Proto:  "tcp",
		Addr:   "0.0.0.0:7111",
		Weight: 10,
	}
	servers = append(servers, s)
	c := conf.RPCServer2{DiscoverOff: true, Token: "", Servers: servers}
	s2 := rpc.NewServer2(&c)
	var r RPC
	err := s2.Register(&r)
	if err != nil {
		panic(err)
	}
	time.Sleep(time.Hour)
}
