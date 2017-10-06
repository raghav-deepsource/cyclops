package com.aol.cyclops2.data.collections.extensions.lazy;


import com.aol.cyclops2.types.foldable.Evaluation;
import cyclops.collections.mutable.DequeX;
import cyclops.collections.mutable.ListX;
import cyclops.stream.ReactiveSeq;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;

import static com.aol.cyclops2.types.foldable.Evaluation.LAZY;

/**
 * An extended List type {@see java.util.List}
 * Extended List operations execute lazily e.g.
 * <pre>
 * {@code
 *    StreamX<Integer> q = StreamX.of(1,2,3)
 *                                      .map(i->i*2);
 * }
 * </pre>
 * The map operation above is not executed immediately. It will only be executed when (if) the data inside the
 * queue is accessed. This allows lazy operations to be chained and executed more efficiently e.g.
 *
 * <pre>
 * {@code
 *    StreamX<Integer> q = StreamX.of(1,2,3)
 *                                      .map(i->i*2);
 *                                      .filter(i->i<5);
 * }
 * </pre>
 *
 * The operation above is more efficient than the equivalent operation with a ListX.
 *
 * @author johnmcclean
 *
 * @param <T> the type of elements held in this toX
 */
public class StreamX<T> extends AbstractLazyCollection<T,List<T>> implements ListX<T> {


    public StreamX(List<T> list, ReactiveSeq<T> seq, Collector<T, ?, List<T>> collector,Evaluation strict) {

        super(list, seq, collector,strict,r->{
            CompletableListX<T> res = new CompletableListX<>();
            r.forEachAsync(l->res.complete(l));
            return res.asListX();
        });


    }
    public StreamX(List<T> list, ReactiveSeq<T> seq, Collector<T, ?, List<T>> collector) {
       this(list, seq, collector, LAZY);

    }

    @Override
    public StreamX<T> withCollector(Collector<T, ?, List<T>> collector){
        return (StreamX)new StreamX<T>(this.getList(),this.getSeq().get(),collector, evaluation());
    }
    //@Override
    public ListX<T> materialize() {
        get();
        return this;
    }

    @Override
    public T getOrElse(int index, T value) {
        List<T> x = get();
        if(index>x.size())
            return value;
        return x.get(index);
    }

    @Override
    public T getOrElseGet(int index, Supplier<? extends T> supplier) {
        List<T> x = get();
        if(index>x.size())
            return supplier.get();
        return x.get(index);
    }

    @Override
    public ListX<T> lazy() {
        return new StreamX<T>(getList(),getSeq().get(),getCollectorInternal(), LAZY) ;
    }

    @Override
    public ListX<T> eager() {
        return new StreamX<T>(getList(),getSeq().get(),getCollectorInternal(),Evaluation.EAGER) ;
    }

    @Override
    public ListX<T> type(Collector<T, ?, List<T>> collector) {
        return withCollector(collector);
    }

    @Override
    public String toString() {
        return get().toString();
    }

    @Override
    public <T1> Collector<T1, ?, List<T1>> getCollector() {
        return (Collector)super.getCollectorInternal();
    }

    @Override
    public void replaceAll(UnaryOperator<T> operator) {
        get().replaceAll(operator);
    }

    @Override
    public void sort(Comparator<? super T> c) {
        get().sort(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        return get().addAll(index,c);
    }

    @Override
    public T get(int index) {
        return get().get(index);
    }

    @Override
    public T set(int index, T element) {
        return get().set(index,element);
    }

    @Override
    public void add(int index, T element) {
        get().add(index,element);
    }

    @Override
    public T remove(int index) {
        return get().remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return get().indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return get().lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return get().listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return get().listIterator(index);
    }
    @Override
    public ListX<T> subList(final int fromIndex, final int toIndex) {
        List<T> list = get().subList(fromIndex, toIndex);
      return from(list);
    }





    @Override
    public Iterator<T> iterator() {
        return get().iterator();
    }

    @Override
    public <X> StreamX<X> fromStream(ReactiveSeq<X> stream) {
        return new StreamX<X>((List) getList(), ReactiveSeq.fromStream(stream), (Collector) this.getCollectorInternal(),this.evaluation());
    }

    @Override
    public <T1> StreamX<T1> from(Collection<T1> c) {
        if(c instanceof List)
            return new StreamX<T1>((List)c,null,(Collector)this.getCollectorInternal(),this.evaluation());
        return fromStream(ReactiveSeq.fromIterable(c));
    }

    @Override
    public <U> StreamX<U> unitIterator(Iterator<U> it) {
        return fromStream(ReactiveSeq.fromIterator(it));
    }




    @Override
    public <R> StreamX<R> unit(Collection<R> col) {
        return from(col);
    }
}