package ch.epfl.lsi.ironICpp;

import java.util.ArrayList;
import java.util.Collections;

public class Filter {
	/**
	 * Filtering is a class providing filtering tools and options
	 */

	/**
	 * Class constants.
	 */
	// Filter names (index should match the order in strings.xml
	public static final int FILTER_MOVING_AVERAGE = 0;
	public static final int FILTER_INFINITE_IMPULSE_RESPONSE = 1;
	public static final int FILTER_KALMAN = 2;
	public static final int FILTER_MEDIAN_VALUE = 3;

	// Filter parameters
	private int filterActive;
	private float filterStrength = 0.1F;

	// Filters
	private float iir(ArrayList<Float> values)
	{
		if(values.size() == 0) return Float.NaN;
		float result = values.get(0);
		int size = values.size();
		for(int i=1; i<size; i++)
		{
			result = (1-filterStrength)*result + filterStrength*values.get(i);
		}
		return result;
	}

	private float movAvg(ArrayList<Float> values)
	{
		float result = 0;
		int size = values.size();
//		Log.d("Filtering", "value size "+values.size());
		if(size == 0)
		{
			return Float.NaN;
		}
		for(int i=0; i<size; i++)
		{
			result += values.get(i);
		}
//		Log.d("Filtering", "value "+result);
		return result/size;
	}

	private float kalman(ArrayList<Float> values)
	{
		// TODO: Implement Kalman filtering
		float result = 0;
		return result;
	}

	private float median(ArrayList<Float> values)
	{
		if(values.size() == 0)
		{
			return Float.NaN;
		}
		Collections.sort(values);
		return values.get((values.size()-1)/2);
	}

	public void setFilterID(int filterID)
	{
		switch(filterID)
		{
			case FILTER_INFINITE_IMPULSE_RESPONSE:
				filterActive = FILTER_INFINITE_IMPULSE_RESPONSE;
				filterStrength = 0.1F;
				break;
			case FILTER_KALMAN:
				filterActive = FILTER_KALMAN;
				break;
			case FILTER_MEDIAN_VALUE:
				filterActive = FILTER_MEDIAN_VALUE;
				break;
			case FILTER_MOVING_AVERAGE:
				filterActive = FILTER_MOVING_AVERAGE;
				break;
		}
	}
	public void setFilter(int filterID, float filterStrength)
	{
		setFilterID(filterID);
		this.filterStrength = filterStrength;
	}

	public void setFilterStrength(float filterStrength)
	{
		this.filterStrength = filterStrength;
	}

	public int getFilterID() {
		return filterActive;
	}

	public float getFilterStrength() {
		return filterStrength;
	}

	public float filter(ArrayList<Float> values)
	{
		// Should be done in another way: there is some duplicated code
		// Maybe move each filter implementation to a subclass?
		switch(filterActive)
		{
			case FILTER_INFINITE_IMPULSE_RESPONSE:
				return iir(values);
			case FILTER_KALMAN:
				return kalman(values);
			case FILTER_MEDIAN_VALUE:
				return median(values);
			case FILTER_MOVING_AVERAGE:
				return movAvg(values);
		}
		return -1;
	}

}
