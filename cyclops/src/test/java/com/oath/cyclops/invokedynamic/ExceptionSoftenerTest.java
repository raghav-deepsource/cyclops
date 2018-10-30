package com.oath.cyclops.invokedynamic;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.function.Supplier;

import com.oath.cyclops.util.ExceptionSoftener;
import org.junit.Test;

public class ExceptionSoftenerTest {

    @Test
    public void castOrThrow(){
        IOException io = new IOException("hello");
        IOException ex =  ExceptionSoftener.castOrThrow(io,IOException.class);
        IOException ex2 =  ExceptionSoftener.castOrThrow(new FileNotFoundException("hello"),IOException.class);
    }

    @Test(expected = ClosedChannelException.class)
    public void castOrThrowError(){
        IOException io = new ClosedChannelException();
        IOException ex =  ExceptionSoftener.castOrThrow(io,FileNotFoundException.class);

    }

    @Test(expected=IOException.class)
	public void checked() {
		throw ExceptionSoftener.throwSoftenedException(new IOException("hello"));
	}
	@Test(expected=Exception.class)
	public void checkedException() {
		throw ExceptionSoftener.throwSoftenedException(new Exception("hello"));
	}
	@Test(expected=RuntimeException.class)
	public void rumtime() {
		throw ExceptionSoftener.throwSoftenedException(new RuntimeException("hello"));
	}

	@Test(expected=IOException.class)
	public void testThrowif(){
		ExceptionSoftener.throwIf(new IOException("hello"), e-> e instanceof IOException);
	}
	@Test
	public void testThrowifFalse(){
		ExceptionSoftener.throwIf(new IOException("hello"), e-> e.getMessage()=="world");
	}
	boolean value = false;
	@Test
	public void testThrowOrHandle(){
		value = false;
		try{
			ExceptionSoftener.throwOrHandle(new IOException("hello"), e-> e instanceof IOException,c->this.value=true);
			fail("should not reach");
		}catch(Exception e){
			assertFalse(value);
		}
	}
	@Test
	public void testThrowifHandle(){
		value = false;
		try{
			ExceptionSoftener.throwOrHandle(new IOException("hello"), e-> e.getMessage()=="world",c->this.value=true);

		}catch(Exception e){
			assertTrue(value);
		}

	}
	private String get() throws IOException{
		return "hello";
	}
	@Test
	public void methodReference(){
		Supplier<String> supplier = ExceptionSoftener.softenSupplier(this::get);

		assertThat(supplier.get(),equalTo("hello"));
	}

	@Test
	public void softenCallable(){
		Supplier<String> supplier = ExceptionSoftener.softenCallable(this::get);

		assertThat(supplier.get(),equalTo("hello"));
	}



}
