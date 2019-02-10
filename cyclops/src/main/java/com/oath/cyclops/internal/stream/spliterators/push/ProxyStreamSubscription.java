package com.oath.cyclops.internal.stream.spliterators.push;

import lombok.Getter;
import lombok.Setter;

import java.util.function.LongConsumer;

public class ProxyStreamSubscription extends StreamSubscription {
    @Override
    public boolean isActive() {
        return sub.isActive();
    }

    @Override
    public boolean singleActiveRequest(long n, LongConsumer work) {
        boolean res = sub.singleActiveRequest(n, work);
        drain();
        return res;
    }

    @Override
    public void request(long n) {
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
        if(from==null)
            return;
        long requested = from.requested.get();
        if(requested>0){
            boolean completed = true;
            do{

                if (Long.MAX_VALUE==requested) {
                    sub.request(requested);
                    from.requested.set(0);
                    completed=true;
                }else {
                    completed = from.requested.compareAndSet(requested, 0);
                    if (completed)
                        sub.request(requested);
                }
            }while(!completed);
        }
    }


    @Getter @Setter
    volatile StreamSubscription sub;

    public void swap(StreamSubscription sub){
        StreamSubscription old  = sub;
        this.sub = sub;
        setFrom(old);
        drain();
        

    }
    public void setFrom(StreamSubscription from){
        this.from = from;
    }

    volatile StreamSubscription from;
}
