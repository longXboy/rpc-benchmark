package main

import (
	"context"
	model "thrift"
)

var defaultCtx = context.Background()

type GreeterHandler struct {
}

func NewGreeterHandler() *GreeterHandler {
	return &GreeterHandler{}
}

func (greeter GreeterHandler) Say(ctx context.Context, name *model.BenchmarkMessage) (r *model.BenchmarkMessage, err error) {
	name.Field1 = "OK"
	name.Field2 = 100
	return name, nil
}
