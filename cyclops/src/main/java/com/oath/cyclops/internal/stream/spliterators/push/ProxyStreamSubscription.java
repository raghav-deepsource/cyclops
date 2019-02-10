package com.oath.cyclops.internal.stream.spliterators.push;

import cyclops.data.tuple.Tuple2;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.LongConsumer;

public class ProxyStreamSubscription extends StreamSubscription {
    @Override
    public boolean isActive() {
        return sub.isActive();
    }

    @Override
    public boolean singleActiveRequest(long n, LongConsumer work) {
        if(!swapped) {
            if (n == Long.MAX_VALUE) {
                demand.set(n);
            } else {
                demand.updateAndGet(i -> {
                    long sum = i + n;
                    return sum < 0L ? Long.MAX_VALUE : sum;
                });
            }
        }

        boolean res = sub.singleActiveRequest(n, work);
        drain();
        return res;
    }

    @Override
    public void request(long n) {
//new sub, but swapped = false
        //add demand, but will already be added to new sub
        if(!swapped) {
            if (n == Long.MAX_VALUE) {
                demand.set(n);
            } else {
                demand.updateAndGet(i -> {
                    long sum = i + n;
                    return sum < 0L ? Long.MAX_VALUE : sum;
                });
            }
        }

        sub.request(n);

        drain();
    }

    @Override
    public void cancel() {
        sub.cancel();
    }

    @Override
    public long getRequested() {
        return sub.getRequested();
    }

    public void drain(){

        if(!swapped)
            return;

        long requested = demand.get();

        if(requested>0){
            boolean completed = true;
            do{

                if (Long.MAX_VALUE==requested) {
                    sub.request(requested);
                    completed=true;
                }else {
                    completed = demand.compareAndSet(requested, 0);
                    if (completed)
                        sub.request(requested);
                }
            }while(!completed);
        }
    }


    Object lock = new Object();
    @Getter @Setter
    volatile StreamSubscription sub;

    public void swap(StreamSubscription next){
        while(this.sub==null){
            LockSupport.parkNanos(10l);
        }
        this.sub = next;
        swapped = true;


        drain();

    }


    AtomicReference<Tuple2<Boolean,StreamSubscription>> status;
    AtomicLong demand = new AtomicLong();
    volatile boolean swapped;
}
