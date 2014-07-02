package org.castaconcord.consensusprocess;

import java.util.concurrent.CyclicBarrier;

public class MockTriggerableThreshold extends ConsensusThreshold {
	CyclicBarrier thresholdBarrier;
	CyclicBarrier canWaitforThresholdBarrier;
	
	public MockTriggerableThreshold(int maxNbSteps, int nbThreads) {
		super(0,maxNbSteps,1,0);
		this.thresholdBarrier = new CyclicBarrier(nbThreads, new Runnable() {
			@Override
			public void run() {
				incrementProposalThreshold();
				canWaitforThresholdBarrier.reset();
			}
		});
		canWaitforThresholdBarrier = new CyclicBarrier(nbThreads, new Runnable() {
			@Override
			public void run() {
				thresholdBarrier.reset();
			}
		});
	}

	@Override
	public boolean awaitNextThresholdIncrement() {
		triggerThresholdIncrease();
		return currentThreshold<thresholdEnd;
	}

	public void triggerThresholdIncrease(){
		try {
			canWaitforThresholdBarrier.await();
			thresholdBarrier.await();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}