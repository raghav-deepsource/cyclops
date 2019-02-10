package com.oath.cyclops.internal.stream.spliterators.push;

import com.oath.cyclops.internal.stream.ReactiveStreamX;
import cyclops.data.Seq;
import cyclops.reactive.ReactiveSeq;
import cyclops.reactive.Spouts;

import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;


public class OnEmptySwitchOperator<T> extends BaseOperator<T,T> {


    private final Supplier<? extends Stream<T>> value;

    public OnEmptySwitchOperator(Operator<T> source, Supplier<? extends Stream<T>> value) {
        super(source);
        this.value = value;


    }
    /**
    @Override
    public StreamSubscription subscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError, Runnable onComplete) {
        Stream<T> stream = value.get();

        Operator<T> op = ((ReactiveStreamX<T>) stream).getSource();

        Seq<Operator<T>> operators = Seq.of(source, op);
        OnEmptySwitch2[] ref = {null};
        StreamSubscription sub = new StreamSubscription() {

            @Override
            public void request(long n) {

                if (n <= 0) {
                    onError.accept(new IllegalArgumentException("3.9 While the Subscription is not cancelled, Subscription.request(long n) MUST throw a java.lang.IllegalArgumentException if the argument is <= 0."));
                    return;
                }


                super.request(n);

                ref[0].request(n);


            }

            @Override
            public void cancel() {
                ref[0].cancel();
                super.cancel();

            }
        };

        OnEmptySwitch2 c = new OnEmptySwitch2<>(sub, operators, onNext, onError, onComplete);
        ref[0] = c;


        return sub;
    }

   **/

    @Override
    public StreamSubscription subscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError, Runnable onComplete) {
        boolean[] data = {false};
        ProxyStreamSubscription proxySubscription = new ProxyStreamSubscription();
        StreamSubscription upstream = source.subscribe(e -> {
                if (!data[0])
                    data[0] = true;
                onNext.accept(e);
            }
            , onError, () -> {
                if (data[0] == false) {


                    Stream<T> stream = value.get();

                    Operator<T> op = ((ReactiveStreamX<T>) stream).getSource();//new PublisherToOperator<>(ReactiveSeq.fromStream(stream));
                    StreamSubscription next = op.subscribe(onNext, onError, onComplete);
                    proxySubscription.swap(next);
                    data[0] = true;

                } else {
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
