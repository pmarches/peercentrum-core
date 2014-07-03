package org.peercentrum.consensusprocess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.peercentrum.consensusprocess.ConsensusThreshold;

public class ConsensusThresholdTest {
	@Test
	public void testMultiThread() throws InterruptedException{
		final int NB_STEP = 5;
		final int NB_THREAD = 3;
		final MockTriggerableThreshold sync = new MockTriggerableThreshold(NB_STEP, NB_THREAD+1);
		final Random rnd = new Random();
		final CountDownLatch loopCounter= new CountDownLatch(NB_STEP*NB_THREAD);

		for(int i=0; i<NB_THREAD; i++){
			new Thread(){
				public void run() {
					do{
						loopCounter.countDown();
						System.out.println("loopCounter="+loopCounter);
						try {
							Thread.sleep(rnd.nextInt(100));
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					} while(sync.awaitNextThresholdIncrement());
				}
			}.start();
		}
		for(int i=0; i<NB_STEP; i++){
			assertEquals(i, sync.currentThreshold);
			System.out.println(sync.awaitNextThresholdIncrement());
		}
		System.out.println(sync.awaitNextThresholdIncrement());
		
		loopCounter.await();
	}
	
	@Test
	public void testTimed() {
		ConsensusThreshold sync = new ConsensusThreshold(50, 100, 10, 1000/5);
		for(int i=0; i<4; i++){
			System.out.println(sync);
			assertTrue(sync.awaitNextThresholdIncrement());
		}
		System.out.println(sync);
		assertFalse(sync.awaitNextThresholdIncrement());
		
	}

}
