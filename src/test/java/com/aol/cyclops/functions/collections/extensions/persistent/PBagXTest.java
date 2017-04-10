package com.aol.cyclops.functions.collections.extensions.persistent;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jooq.lambda.tuple.Tuple2;
import org.junit.Test;

import com.aol.cyclops.control.ReactiveSeq;
import com.aol.cyclops.data.collections.extensions.FluentCollectionX;
import com.aol.cyclops.data.collections.extensions.persistent.PBagX;
import com.aol.cyclops.functions.collections.extensions.AbstractCollectionXTest;

public class PBagXTest extends AbstractCollectionXTest{

	@Override
	public <T> FluentCollectionX<T> of(T... values) {
		return PBagX.of(values);
	}
	@Test
    public void onEmptySwitch(){
            assertThat(PBagX.empty().onEmptySwitch(()->PBagX.of(1,2,3)),equalTo(PBagX.of(1,2,3)));
    }
	@Test
    public void coflatMap(){
       assertThat(PBagX.of(1,2,3)
                   .coflatMap(s->s.sum().get())
                   .single(),equalTo(6));
        
    }
	/* (non-Javadoc)
	 * @see com.aol.cyclops.functions.collections.extensions.AbstractCollectionXTest#empty()
	 */
	@Override
	public <T> FluentCollectionX<T> empty() {
		return PBagX.empty();
	}
    @Override
    public FluentCollectionX<Integer> range(int start, int end) {
        return PBagX.range(start, end);
    }
    @Override
    public FluentCollectionX<Long> rangeLong(long start, long end) {
        return PBagX.rangeLong(start, end);
    }
    @Override
    public <T> FluentCollectionX<T> iterate(int times, T seed, UnaryOperator<T> fn) {
       return PBagX.iterate(times, seed, fn);
    }
    @Override
    public <T> FluentCollectionX<T> generate(int times,  Supplier<T> fn) {
       return PBagX.generate(times, fn);
    }
    @Override
    public <U, T> FluentCollectionX<T> unfold(U seed, Function<? super U, Optional<Tuple2<T, U>>> unfolder) {
       return PBagX.unfold(seed, unfolder);
    }

}