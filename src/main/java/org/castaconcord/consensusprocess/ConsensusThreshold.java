package org.castaconcord.consensusprocess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides a "pacing" mechanism for database closes. This mechanism needs to
 * be tunable for databases that are closed at different speeds. It also needs to be made 
 * fast for unit testing purposes. This can be achieved by subclassing into a mock object.
 */
public class ConsensusThreshold {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConsensusThreshold.class);
	
	long lastThresholdIncrementTS;
	int nbMSPerIncrement;
	protected int currentThreshold;
	protected int thresholdStart;
	protected int thresholdEnd;
	protected int thresholdStep;
	public Object thresholdReachedMonitor=new Object();
	
	public ConsensusThreshold(int thresholdStart, int thresholdEnd, int thresholdStep, int nbMSPerIncrement) {
		this.currentThreshold=thresholdStart;
		this.thresholdStart=thresholdStart;
		this.thresholdEnd=thresholdEnd;
		this.thresholdStep=thresholdStep;
		
		this.nbMSPerIncrement=nbMSPerIncrement;
		this.lastThresholdIncrementTS=System.currentTimeMillis();
	}

	/**
	 * @return true if thresholdMax has not been reached yet.
	 */
	public boolean awaitNextThresholdIncrement() {
		try {
			int nbMsElapsedSinceLastIncrement=(int) (System.currentTimeMillis()-lastThresholdIncrementTS);
			int nbMsUntilNextIncrement=nbMSPerIncrement-nbMsElapsedSinceLastIncrement;
			nbMsUntilNextIncrement=Math.max(nbMsUntilNextIncrement, 0);
			Thread.sleep(nbMsUntilNextIncrement);
			lastThresholdIncrementTS=System.currentTimeMillis();
			return incrementProposalThreshold();
		} catch (InterruptedException e) {
			LOGGER.error("awaitNextThresholdIncrement", e);
			return false;
		}
	}


	public boolean incrementProposalThreshold() {
		currentThreshold+=thresholdStep;
		if(currentThreshold<thresholdEnd){
			return true;
		}
		currentThreshold=thresholdStart;
		synchronized(thresholdReachedMonitor){
			thresholdReachedMonitor.notifyAll();
		}
		return false;
	}

	public int getNbValidatorRequired(int nbValidatorAvailable) {
		float currentThresholdFloat=(float) currentThreshold/thresholdEnd;
		return (int) (currentThresholdFloat*nbValidatorAvailable);
	}
	
	@Override
	public String toString() {
		return "currentThreshold="+currentThreshold;
	}
}
