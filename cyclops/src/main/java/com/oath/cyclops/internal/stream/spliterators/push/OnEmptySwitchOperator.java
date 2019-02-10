package com.oath.cyclops.internal.stream.spliterators.push;

import cyclops.reactive.ReactiveSeq;
import cyclops.reactive.Spouts;

import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;


public class OnEmptySwitchOperator<T> extends BaseOperator<T,T> {


    private final Supplier<? extends Stream<T>> value;

    public OnEmptySwitchOperator(Operator<T> source, Supplier<? extends Stream<T>> value){
        super(source);
        this.value = value;


    }


    @Override
    public StreamSubscription subscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError, Runnable onComplete) {
        boolean[] data ={ false};
        ProxyStreamSubscription proxySubscription = new ProxyStreamSubscription();
        StreamSubscription upstream =  source.subscribe(e->{
                if(!data[0])
                    data[0]=true;
                onNext.accept(e);
            }
            ,onError,()->{
                if(data[0]==false) {

                    Stream<T> stream = value.get();
                    PublisherToOperator<T> op = new PublisherToOperator<>(ReactiveSeq.fromStream(stream));
                    StreamSubscription next = op.subscribe(onNext,onError,onComplete);
                    proxySubscription.swap(next);
                    proxySubscription.request(1);
                    data[0]=true;

                }else {
                    onComplete.run();
                }
            });
        proxySubscription.setSub(upstream);
        return proxySubscription;
    }

    @Override
    public void subscribeAll(Consumer<? super T> onNext, Consumer<? super Throwable> onError, Runnable onCompleteDs) {

        boolean[] data ={ false};
        source.subscribeAll(e->{
                    if(!data[0])
                     data[0]=true;
                    onNext.accept(e);
                }
                ,onError,()->{
                        if(data[0]==false) {
                            Stream<T> stream = value.get();
                            PublisherToOperator<T> op = new PublisherToOperator<>(ReactiveSeq.fromStream(stream));
                            op.subscribeAll(onNext,onError,onCompleteDs);
                        }else {
                            onCompleteDs.run();
                        }
                });
    }
}
