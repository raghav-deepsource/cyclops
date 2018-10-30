package cyclops.control;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.oath.cyclops.types.foldable.To;
import com.oath.cyclops.types.Value;
import cyclops.data.tuple.Tuple4;
import cyclops.data.tuple.Tuple5;
import cyclops.data.tuple.Tuple6;
import cyclops.data.tuple.Tuple7;
import cyclops.function.Function0;
import cyclops.function.Function3;
import cyclops.data.tuple.Tuple;
import cyclops.data.tuple.Tuple2;
import cyclops.data.tuple.Tuple3;
import cyclops.function.Function4;
import cyclops.function.Function5;
import cyclops.function.Function6;
import cyclops.function.Function7;
import cyclops.reactive.ReactiveSeq;

import static cyclops.data.tuple.Tuple.tuple;

/**
 * simple Trampoline implementation : inspired by excellent TotallyLazy Java 8 impl
 * and Mario Fusco presentation
 *
 * Allows Stack Free Recursion
 *
 * <pre>
 * {@code
 * @Test
    public void trampolineTest(){

        assertThat(loop(500000,10).result(),equalTo(446198426));

    }
    Trampoline<Integer> loop(int times,int sum){

        if(times==0)
            return Trampoline.done(sum);
        else
            return Trampoline.more(()->loop(times-1,sum+times));
    }
 *
 * }
 * </pre>
 *
 * And co-routines can be implemented simply via zipping trampolines
 *
    <pre>
 {@code
 Trampoline<Integer> looping = loop(500000,5);
 Trampoline<Integer> looping2 = loop2(500000,5);
 System.out.println(looping.zip(looping2).getValue());

 }
    </pre>

 Where loop and loop2 are implemented recursively using Trampoline with additional print logic


 <pre>
 {@code
 Trampoline<Integer> loop(int times,int sum){
    System.out.println("Loop-A " + times + " : " + sum);
    if(times==0)
         return Trampoline.done(sum);
    else
        return Trampoline.more(()->loop(times-1,sum+times));
    }
 }

 </pre>

 Results in interleaved execution visible from the console
 <pre>
...
 Loop-B 21414 : 216908016
 Loop-A 21413 : 216929430
 Loop-B 21413 : 216929430
 Loop-A 21412 : 216950843
...

 </pre>
 *
 * @author johnmcclean
 *
 * @param <T> Return type
 */
@FunctionalInterface
public interface Trampoline<T> extends Value<T>, Function0<T>,To<Trampoline<T>> {

    public static <T> Trampoline<T> narrow(Trampoline<? extends T> broad){
        return (Trampoline<T>)broad;
    }
    default <R> R fold(Function<? super Trampoline<T>,? extends R> more, Function<? super T, ? extends R> done){
        return complete() ? done.apply(get()) : more.apply(this.bounce());
    }

    default  <B> Trampoline<Tuple2<T,B>> zip(Trampoline<B> b){
        return zip(b,(x,y)->Tuple.tuple(x,y));

    }
    @Override
    default <R>  Trampoline<R> mapFn(Function<? super T, ? extends R> fn){
      return map(fn);
    }
    default <R>  Trampoline<R> map(Function<? super T, ? extends R> fn){
      Either<Trampoline<T>,T> e = resume();
      return e.fold(left->{
        return Trampoline.more(()->left.map(fn));
      },right->{
        return Trampoline.done(fn.apply(right));
      });
    }
    default <R>  Trampoline<R> flatMap(Function<? super T, ? extends Trampoline<R>> fn){
      Either<Trampoline<T>,T> e = resume();
      return e.fold(left->{
        return Trampoline.more(()->left.flatMap(fn));
      },right->{
        return fn.apply(right);
      });
    }
    default  <B,R> Trampoline<R> zip(Trampoline<B> b,BiFunction<? super T,? super B,? extends R> zipper){

        Either<Trampoline<T>,T> first = resume();
        Either<Trampoline<B>,B> second = b.resume();

        if(first.isLeft() && second.isLeft()) {
            return Trampoline.more(()->first.leftOrElse(null).zip(second.leftOrElse(null),zipper));
        }
        if(first.isRight() && second.isRight()){
            return Trampoline.done(zipper.apply(first.orElse(null),second.orElse(null)));
        }
        if(first.isLeft() && second.isRight()){
            return Trampoline.more(()->first.leftOrElse(null).zip(b,zipper));
        }
        if(first.isRight() && second.isLeft()){
            return Trampoline.more(()->this.zip(second.leftOrElse(null),zipper));
        }
        //unreachable
        return null;

    }
    default  <B,C> Trampoline<Tuple3<T,B,C>> zip(Trampoline<B> b, Trampoline<C> c){
        return zip(b,c,(x,y,z)->Tuple.tuple(x,y,z));

    }
    default  <B,C,R> Trampoline<R> zip(Trampoline<B> b, Trampoline<C> c, Function3<? super T, ? super B, ? super C,? extends R> fn){

        Either<Trampoline<T>,T> first = resume();
        Either<Trampoline<B>,B> second = b.resume();
        Either<Trampoline<C>,C> third = c.resume();

        if(first.isLeft() && second.isLeft() && third.isLeft()) {
            return Trampoline.more(()->first.leftOrElse(null).zip(second.leftOrElse(null),third.leftOrElse(null),fn));
        }
        if(first.isRight() && second.isRight() && third.isRight()){
            return Trampoline.done(fn.apply(first.orElse(null),second.orElse(null),third.orElse(null)));
        }

        if(first.isLeft() && second.isRight() && third.isRight()){
            return Trampoline.more(()->first.leftOrElse(null).zip(b,c,fn));
        }
        if(first.isRight() && second.isLeft() && third.isRight()){
            return Trampoline.more(()->this.zip(second.leftOrElse(null),c,fn));
        }
        if(first.isRight() && second.isRight() && third.isLeft()){
            return Trampoline.more(()->this.zip(b,third.leftOrElse(null),fn));
        }


        if(first.isRight() && second.isLeft() && third.isLeft()){
            return Trampoline.more(()->this.zip(second.leftOrElse(null),third.leftOrElse(null),fn));
        }
        if(first.isLeft() && second.isRight() && third.isLeft()){
            return Trampoline.more(()->first.leftOrElse(null).zip(b,third.leftOrElse(null),fn));
        }
        if(first.isLeft() && second.isLeft() && third.isRight()){
            return Trampoline.more(()->first.leftOrElse(null).zip(second.leftOrElse(null),c,fn));
        }
        //unreachable
        return null;
    }

    default Either<Trampoline<T>,T> resume(){
        return this.fold(Either::left, Either::right);
    }



    /**
     * @return next stage in Trampolining
     */
    default Trampoline<T> bounce() {
        return this;
    }

    /**
     * @return The result of Trampoline execution
     */
    default T result() {
        return get();
    }

    /* (non-Javadoc)
     * @see java.util.function.Supplier#getValue()
     */
    @Override
    T get();


    /* (non-Javadoc)
     * @see com.oath.cyclops.types.Value#iterator()
     */
    @Override
    default Iterator<T> iterator() {
        return Arrays.asList(result())
                     .iterator();
    }

    @Override
    default ReactiveSeq<T> stream() {
        return Function0.super.stream();
    }

    /**
     * @return true if complete
     *
     */
    default boolean complete() {
        return true;
    }

    /**
     * Created a completed Trampoline
     *
     * @param result Completed result
     * @return Completed Trampoline
     */
    public static <T> Trampoline<T> done(final T result) {
        return () -> result;
    }


    /**
     * Create a Trampoline that has more work to do
     *
     * @param trampoline Next stage in Trampoline
     * @return Trampoline with more work
     */
    public static <T> Trampoline<T> more(final Trampoline<Trampoline<T>> trampoline) {
        return new Trampoline<T>() {


            @Override
            public boolean complete() {
                return false;
            }

            @Override
            public Trampoline<T> bounce() {
                return trampoline.result();
            }

            @Override
            public T get() {
                return trampoline(this);
            }

            T trampoline(final Trampoline<T> trampoline) {

                return Stream.iterate(trampoline, Trampoline::bounce)
                             .filter(Trampoline::complete)
                             .findFirst()
                             .get()
                             .result();

            }
        };
    }


    @Override
    default <R> R fold(Function<? super T, ? extends R> present, Supplier<? extends R> absent){
        return present.apply(get());
    }

    public static class Comprehensions {
        public static <T, F, R1, R2, R3, R4,R5,R6,R7> Trampoline<R7> forEach8(Trampoline<T> io,
                                                                              Function<? super T, Trampoline<R1>> value2,
                                                                              BiFunction<? super T, ? super R1, Trampoline<R2>> value3,
                                                                              Function3<? super T, ? super R1,? super  R2, Trampoline<R3>> value4,
                                                                              Function4<? super T, ? super R1,? super R2,? super R3, Trampoline<R4>> value5,
                                                                              Function5<? super T, ? super R1,? super R2,? super R3, ? super R4, Trampoline<R5>> value6,
                                                                              Function6<? super T, ? super R1,? super R2,? super R3, ? super R4,? super R5, Trampoline<R6>> value7,
                                                                              Function7<? super T, ? super R1,? super R2,? super R3, ? super R4,? super R5, ? super R6, Trampoline<R7>> value8)

        {

            return io.flatMap(in -> {

                Trampoline<R1> a = value2.apply(in);
                return a.flatMap(ina -> {
                    Trampoline<R2> b = value3.apply(in, ina);
                    return b.flatMap(inb -> {

                        Trampoline<R3> c = value4.apply(in, ina, inb);

                        return c.flatMap(inc -> {
                            Trampoline<R4> d = value5.apply(in, ina, inb, inc);
                            return d.flatMap(ind->{
                                Trampoline<R5> e = value6.apply(in,ina,inb,inc,ind);
                                return e.flatMap(ine->{
                                    Trampoline<R6> f = value7.apply(in,ina,inb,inc,ind,ine);
                                    return f.flatMap(inf->{
                                        Trampoline<R7> g = value8.apply(in,ina,inb,inc,ind,ine,inf);
                                        return g;
                                    });
                                });
                            });
                        });

                    });


                });


            });

        }
        public static <T, F, R1, R2, R3, R4, R5, R6, R7> Trampoline<R7> forEach(Trampoline<T> io,
                                                                                Function<? super T, Trampoline<R1>> value2,
                                                                                Function<? super Tuple2<T, R1>, Trampoline<R2>> value3,
                                                                                Function<? super Tuple3<T, R1, R2>, Trampoline<R3>> value4,
                                                                                Function<? super Tuple4<T, R1, R2, R3>, Trampoline<R4>> value5,
                                                                                Function<? super Tuple5<T,R1,R2, R3, R4>, Trampoline<R5>> value6,
                                                                                Function<? super Tuple6<T,R1,R2,R3, R4,R5>, Trampoline<R6>> value7,
                                                                                Function<? super Tuple7<T,R1,R2, R3, R4,R5, R6>, Trampoline<R7>> value8
        ) {

            return io.flatMap(in -> {

                Trampoline<R1> a = value2.apply(in);
                return a.flatMap(ina -> {
                    Trampoline<R2> b = value3.apply(Tuple.tuple(in, ina));
                    return b.flatMap(inb -> {

                        Trampoline<R3> c = value4.apply(Tuple.tuple(in, ina, inb));

                        return c.flatMap(inc -> {
                            Trampoline<R4> d = value5.apply(Tuple.tuple(in, ina, inb, inc));
                            return d.flatMap(ind -> {
                                Trampoline<R5> e = value6.apply(Tuple.tuple(in, ina, inb, inc, ind));
                                return e.flatMap(ine -> {
                                    Trampoline<R6> f = value7.apply(Tuple.tuple(in, ina, inb, inc, ind, ine));
                                    return f.flatMap(inf -> {
                                        Trampoline<R7> g = value8.apply(Tuple.tuple(in, ina, inb, inc, ind, ine, inf));
                                        return g;

                                    });

                                });
                            });

                        });

                    });


                });


            });

        }
        public static <T, F, R1, R2, R3, R4,R5,R6> Trampoline<R6> forEach7(Trampoline<T> io,
                                                                           Function<? super T, Trampoline<R1>> value2,
                                                                           BiFunction<? super T, ? super R1, Trampoline<R2>> value3,
                                                                           Function3<? super T, ? super R1,? super  R2, Trampoline<R3>> value4,
                                                                           Function4<? super T, ? super R1,? super R2,? super R3, Trampoline<R4>> value5,
                                                                           Function5<? super T, ? super R1,? super R2,? super R3, ? super R4, Trampoline<R5>> value6,
                                                                           Function6<? super T, ? super R1,? super R2,? super R3, ? super R4,? super R5, Trampoline<R6>> value7)

        {

            return io.flatMap(in -> {

                Trampoline<R1> a = value2.apply(in);
                return a.flatMap(ina -> {
                    Trampoline<R2> b = value3.apply(in, ina);
                    return b.flatMap(inb -> {

                        Trampoline<R3> c = value4.apply(in, ina, inb);

                        return c.flatMap(inc -> {
                            Trampoline<R4> d = value5.apply(in, ina, inb, inc);
                            return d.flatMap(ind->{
                                Trampoline<R5> e = value6.apply(in,ina,inb,inc,ind);
                                return e.flatMap(ine->{
                                    Trampoline<R6> f = value7.apply(in,ina,inb,inc,ind,ine);
                                    return f;
                                });
                            });
                        });

                    });


                });


            });

        }
        public static <T, F, R1, R2, R3, R4, R5, R6> Trampoline<R6> forEach(Trampoline<T> io,
                                                                            Function<? super T, Trampoline<R1>> value2,
                                                                            Function<? super Tuple2<T, R1>, Trampoline<R2>> value3,
                                                                            Function<? super Tuple3<T, R1, R2>, Trampoline<R3>> value4,
                                                                            Function<? super Tuple4<T, R1, R2, R3>, Trampoline<R4>> value5,
                                                                            Function<? super Tuple5<T, R1, R2, R3, R4>, Trampoline<R5>> value6,
                                                                            Function<? super Tuple6<T,  R1, R2,  R3, R4, R5>, Trampoline<R6>> value7
        ) {

            return io.flatMap(in -> {

                Trampoline<R1> a = value2.apply(in);
                return a.flatMap(ina -> {
                    Trampoline<R2> b = value3.apply(Tuple.tuple(in, ina));
                    return b.flatMap(inb -> {

                        Trampoline<R3> c = value4.apply(Tuple.tuple(in, ina, inb));

                        return c.flatMap(inc -> {
                            Trampoline<R4> d = value5.apply(Tuple.tuple(in, ina, inb, inc));
                            return d.flatMap(ind -> {
                                Trampoline<R5> e = value6.apply(Tuple.tuple(in, ina, inb, inc, ind));
                                return e.flatMap(ine -> {
                                    Trampoline<R6> f = value7.apply(Tuple.tuple(in, ina, inb, inc, ind, ine));
                                    return f;
                                });
                            });

                        });

                    });


                });


            });

        }

        public static <T, F, R1, R2, R3, R4,R5> Trampoline<R5> forEach6(Trampoline<T> io,
                                                                        Function<? super T, Trampoline<R1>> value2,
                                                                        BiFunction<? super T, ? super R1, Trampoline<R2>> value3,
                                                                        Function3<? super T, ? super R1,? super  R2, Trampoline<R3>> value4,
                                                                        Function4<? super T, ? super R1,? super R2,? super R3, Trampoline<R4>> value5,
                                                                        Function5<? super T, ? super R1,? super R2,? super R3, ? super R4, Trampoline<R5>> value6)

        {

            return io.flatMap(in -> {

                Trampoline<R1> a = value2.apply(in);
                return a.flatMap(ina -> {
                    Trampoline<R2> b = value3.apply(in, ina);
                    return b.flatMap(inb -> {

                        Trampoline<R3> c = value4.apply(in, ina, inb);

                        return c.flatMap(inc -> {
                            Trampoline<R4> d = value5.apply(in, ina, inb, inc);
                            return d.flatMap(ind->{
                                Trampoline<R5> e = value6.apply(in,ina,inb,inc,ind);
                                return e;
                            });
                        });

                    });


                });


            });

        }
        public static <T, F, R1, R2, R3, R4, R5> Trampoline<R5> forEach(Trampoline<T> io,
                                                                        Function<? super T, Trampoline<R1>> value2,
                                                                        Function<? super Tuple2<T, R1>, Trampoline<R2>> value3,
                                                                        Function<? super Tuple3<T, R1,R2>, Trampoline<R3>> value4,
                                                                        Function<? super Tuple4<T, R1,R2,  R3>, Trampoline<R4>> value5,
                                                                        Function<? super Tuple5<T, R1, R2, R3, R4>, Trampoline<R5>> value6
        ) {

            return io.flatMap(in -> {

                Trampoline<R1> a = value2.apply(in);
                return a.flatMap(ina -> {
                    Trampoline<R2> b = value3.apply(Tuple.tuple(in, ina));
                    return b.flatMap(inb -> {

                        Trampoline<R3> c = value4.apply(Tuple.tuple(in, ina, inb));

                        return c.flatMap(inc -> {
                            Trampoline<R4> d = value5.apply(Tuple.tuple(in, ina, inb, inc));
                            return d.flatMap(ind -> {
                                Trampoline<R5> e = value6.apply(Tuple.tuple(in, ina, inb, inc, ind));
                                return e;
                            });
                        });

                    });


                });


            });

        }

        public static <T, F, R1, R2, R3, R4> Trampoline<R4> forEach5(Trampoline<T> io,
                                                                     Function<? super T, Trampoline<R1>> value2,
                                                                     BiFunction<? super T, ? super R1, Trampoline<R2>> value3,
                                                                     Function3<? super T, ? super R1,? super  R2, Trampoline<R3>> value4,
                                                                     Function4<? super T, ? super R1,? super R2,? super R3, Trampoline<R4>> value5

        ) {

            return io.flatMap(in -> {

                Trampoline<R1> a = value2.apply(in);
                return a.flatMap(ina -> {
                    Trampoline<R2> b = value3.apply(in, ina);
                    return b.flatMap(inb -> {

                        Trampoline<R3> c = value4.apply(in, ina, inb);

                        return c.flatMap(inc -> {
                            Trampoline<R4> d = value5.apply(in, ina, inb, inc);
                            return d;
                        });

                    });


                });


            });

        }
        public static <T, F, R1, R2, R3, R4> Trampoline<R4> forEach(Trampoline<T> io,
                                                                    Function<? super T, Trampoline<R1>> value2,
                                                                    Function<? super Tuple2<T, R1>, Trampoline<R2>> value3,
                                                                    Function<? super Tuple3<T, R1, R2>, Trampoline<R3>> value4,
                                                                    Function<? super Tuple4<T, R1, R2, R3>, Trampoline<R4>> value5

        ) {

            return io.flatMap(in -> {

                Trampoline<R1> a = value2.apply(in);
                return a.flatMap(ina -> {
                    Trampoline<R2> b = value3.apply(Tuple.tuple(in, ina));
                    return b.flatMap(inb -> {

                        Trampoline<R3> c = value4.apply(Tuple.tuple(in, ina, inb));

                        return c.flatMap(inc -> {
                            Trampoline<R4> d = value5.apply(Tuple.tuple(in, ina, inb, inc));
                            return d;
                        });

                    });


                });


            });

        }

        public static <T, F, R1, R2, R3> Trampoline<R3> forEach4(Trampoline<T> io,
                                                                 Function<? super T, Trampoline<R1>> value2,
                                                                 BiFunction<? super T, ? super R1, Trampoline<R2>> value3,
                                                                 Function3<? super T, ? super R1,? super  R2, Trampoline<R3>> value4

        ) {

            return io.flatMap(in -> {

                Trampoline<R1> a = value2.apply(in);
                return a.flatMap(ina -> {
                    Trampoline<R2> b = value3.apply(in, ina);
                    return b.flatMap(inb -> {

                        Trampoline<R3> c = value4.apply(in, ina, inb);

                        return c;

                    });


                });


            });

        }
        public static <T, F, R1, R2, R3> Trampoline<R3> forEach(Trampoline<T> io,
                                                                Function<? super T, Trampoline<R1>> value2,
                                                                Function<? super Tuple2<T,R1>, Trampoline<R2>> value3,
                                                                Function<? super Tuple3<T, R1, R2>, Trampoline<R3>> value4

        ) {

            return io.flatMap(in -> {

                Trampoline<R1> a = value2.apply(in);
                return a.flatMap(ina -> {
                    Trampoline<R2> b = value3.apply(Tuple.tuple(in, ina));
                    return b.flatMap(inb -> {

                        Trampoline<R3> c = value4.apply(Tuple.tuple(in, ina, inb));

                        return c;

                    });


                });


            });

        }
        public static <T, F, R1, R2> Trampoline<R2> forEach3(Trampoline<T> io,
                                                             Function<? super T, Trampoline<R1>> value2,
                                                             BiFunction<? super T, ? super R1, Trampoline<R2>> value3

        ) {

            return io.flatMap(in -> {

                Trampoline<R1> a = value2.apply(in);
                return a.flatMap(ina -> {
                    Trampoline<R2> b = value3.apply(in, ina);
                    return b;


                });


            });

        }
        public static <T, F, R1, R2> Trampoline<R2> forEach(Trampoline<T> io,
                                                            Function<? super T, Trampoline<R1>> value2,
                                                            Function<? super Tuple2<T, R1>, Trampoline<R2>> value3

        ) {

            return io.flatMap(in -> {

                Trampoline<R1> a = value2.apply(in);
                return a.flatMap(ina -> {
                    Trampoline<R2> b = value3.apply(Tuple.tuple(in, ina));
                    return b;


                });


            });

        }
        public static <T, F, R1> Trampoline<R1> forEach2(Trampoline<T> io,
                                                         Function<? super T, Trampoline<R1>> value2

        ) {

            return io.flatMap(in -> {

                Trampoline<R1> a = value2.apply(in);
                return a;


            });

        }
        public static <T, F, R1> Trampoline<R1> forEach(Trampoline<T> io,
                                                        Function<? super T, Trampoline<R1>> value2


        ) {

            return io.flatMap(in -> {

                Trampoline<R1> a = value2.apply(in);
                return a;


            });

        }


    }
}
