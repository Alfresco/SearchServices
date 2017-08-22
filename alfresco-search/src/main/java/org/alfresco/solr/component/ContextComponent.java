package org.alfresco.solr.component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.apache.lucene.index.Fields;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRefBuilder;
import org.apache.lucene.util.StringHelper;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.StrField;

/**
 * Return TermEnum information, useful for things like auto suggest.
 * 
 * <pre class="prettyprint">
 * &lt;searchComponent name="termsComponent" class="solr.TermsComponent"/&gt;
 * 
 * &lt;requestHandler name="/terms" class="solr.SearchHandler"&gt;
 *   &lt;lst name="defaults"&gt;
 *     &lt;bool name="terms"&gt;true&lt;/bool&gt;
 *   &lt;/lst&gt;
 *   &lt;arr name="components"&gt;
 *     &lt;str&gt;termsComponent&lt;/str&gt;
 *   &lt;/arr&gt;
 * &lt;/requestHandler&gt;
 * </pre>
 *
 * @see org.apache.solr.common.params.TermsParams See Lucene's TermEnum class
 *
 */
public class ContextComponent extends SearchComponent {
	public static final int UNLIMITED_MAX_COUNT = -1;
	public static final String COMPONENT_NAME = "terms";

	public static final String CONTEXT = "context";
	public static final String CONTEXT_FIELD = "context.fl";
	public static final String CONTEXT_PREFIX_STR = "context.prefix";
	public static final String CONTEXT_LIMIT = "context.limit";
	public static final String CONTEXT_MODE = "context.mode";
	public static final String CONTEXT_PMI_POWER = "context.pmi.power";
	public static final String CONTEXT_PMI_SHIFT = "context.pmi.shift";
	public static final String CONTEXT_PMI_MIN_TF = "context.pmi.min.tf";
	public static final String CONTEXT_PMI_MIN_CF = "context.pmi.min.cf";
	public static final String CONTEXT_PMI_NORMALISE = "context.pmi.normalise";
	public static final String CONTEXT_RERANK = "context.rerank";

	private static ConcurrentHashMap<String, ContextAccumulator> blackList = new ConcurrentHashMap<String, ContextAccumulator>(
			100);
	
	private static ConcurrentHashMap<String, ContextAccumulator2> cache = new ConcurrentHashMap<String, ContextAccumulator2>(
			200000);

	private static ContextAccumulator2 termCounts;

	@Override
	public void prepare(ResponseBuilder rb) throws IOException {

	}

	@Override
	public void process(ResponseBuilder rb) throws IOException {
		SolrParams params = rb.req.getParams();
		if (!params.get(CONTEXT, "false").equals("true")) {
			return;
		}

		String[] fields = params.getParams(CONTEXT_FIELD);

		if (fields == null || fields.length == 0)
			return;

		String[] prefixes = params.getParams(CONTEXT_PREFIX_STR);

		if (prefixes.length == 1) {
			predict(rb);
		} else if (prefixes.length == 2) {
			cosineSimilarity(rb);
		} else if (prefixes.length == 3) {
			isTo(rb, null, null, null);
		} else if (prefixes.length == 4) {
			oddOneOut(rb, null, null, null, null);
		}

	}

	private void oddOneOut(ResponseBuilder rb, String one, String two, String three, String four) throws IOException {
		SolrParams params = rb.req.getParams();
		int limit = params.getInt(CONTEXT_LIMIT, 100);

		String[] fields = params.getParams(CONTEXT_FIELD);

		if (fields == null || fields.length != 1) {
			return;
		}
		String field = fields[0];

		String prefix1 = one;
		String prefix2 = two;
		String prefix3 = three;
		String prefix4 = four;
		if ((one == null) && (two == null)) {

			String[] prefixes = params.getParams(CONTEXT_PREFIX_STR);
			if (prefixes == null || prefixes.length != 4) {
				return;
			}

			prefix1 = prefixes[0];
			prefix2 = prefixes[1];
			prefix3 = prefixes[2];
			prefix4 = prefixes[3];
		}

		final LeafReader indexReader = rb.req.getSearcher().getSlowAtomicReader();
		Fields lfields = indexReader.fields();

		Terms terms = lfields.terms(field);
		if (terms == null) {
			// field does not exist
			return;
		}

		long sumTotalTermFreq = terms.getSumTotalTermFreq();
		if (sumTotalTermFreq == -1) {
			throw new IllegalStateException();
		}

		FieldType ft = rb.req.getSchema().getFieldTypeNoEx(field);
		if (ft == null)
			ft = new StrField();

		ContextAccumulator wordToContext1 = getTop(rb.req.getSearcher(), ft, field, prefix1);
		ContextAccumulator wordToContext2 = getTop(rb.req.getSearcher(), ft, field, prefix2);
		ContextAccumulator wordToContext3 = getTop(rb.req.getSearcher(), ft, field, prefix3);
		ContextAccumulator wordToContext4 = getTop(rb.req.getSearcher(), ft, field, prefix4);

		ArrayList<ValueAndWeight<String>> answer = new ArrayList<ValueAndWeight<String>>();

		double sim_1_2 = 1 - getCosineSimilarity(wordToContext1, wordToContext2);
		double sim_1_3 = 1 - getCosineSimilarity(wordToContext1, wordToContext3);
		double sim_1_4 = 1 - getCosineSimilarity(wordToContext1, wordToContext4);
		double sim_2_1 = 1 - getCosineSimilarity(wordToContext2, wordToContext1);
		double sim_2_3 = 1 - getCosineSimilarity(wordToContext2, wordToContext3);
		double sim_2_4 = 1 - getCosineSimilarity(wordToContext2, wordToContext4);
		double sim_3_1 = 1 - getCosineSimilarity(wordToContext3, wordToContext1);
		double sim_3_2 = 1 - getCosineSimilarity(wordToContext3, wordToContext2);
		double sim_3_4 = 1 - getCosineSimilarity(wordToContext3, wordToContext4);
		double sim_4_1 = 1 - getCosineSimilarity(wordToContext4, wordToContext1);
		double sim_4_2 = 1 - getCosineSimilarity(wordToContext4, wordToContext2);
		double sim_4_3 = 1 - getCosineSimilarity(wordToContext4, wordToContext3);

		answer.add(new ValueAndWeight<>(prefix1, Math.pow(sim_1_2, 2) + Math.pow(sim_1_3, 2) + Math.pow(sim_1_4, 2)));
		answer.add(new ValueAndWeight<>(prefix2, Math.pow(sim_2_1, 2) + Math.pow(sim_2_3, 2) + Math.pow(sim_2_4, 2)));
		answer.add(new ValueAndWeight<>(prefix3, Math.pow(sim_3_1, 2) + Math.pow(sim_3_2, 2) + Math.pow(sim_3_4, 2)));
		answer.add(new ValueAndWeight<>(prefix4, Math.pow(sim_4_1, 2) + Math.pow(sim_4_2, 2) + Math.pow(sim_4_3, 2)));

		answer.sort(null);

		NamedList<Object> oddOneOut = new SimpleOrderedMap<>();
		rb.rsp.add("oddOneOut", oddOneOut);
		for (ValueAndWeight<String> candidate : answer) {
			oddOneOut.add(candidate.key, candidate.weight);
		}
	}

	private void isTo(ResponseBuilder rb, String one, String two, String three) throws IOException {
		SolrParams params = rb.req.getParams();
		int limit = params.getInt(CONTEXT_LIMIT, 100);

		String[] fields = params.getParams(CONTEXT_FIELD);

		if (fields == null || fields.length != 1) {
			return;
		}
		String field = fields[0];

		String prefix1 = one;
		String prefix2 = two;
		String prefix3 = three;
		if ((one == null) && (two == null)) {

			String[] prefixes = params.getParams(CONTEXT_PREFIX_STR);
			if (prefixes == null || prefixes.length != 3) {
				return;
			}

			prefix1 = prefixes[0];
			prefix2 = prefixes[1];
			prefix3 = prefixes[2];

		}

		final LeafReader indexReader = rb.req.getSearcher().getSlowAtomicReader();
		Fields lfields = indexReader.fields();

		Terms terms = lfields.terms(field);
		if (terms == null) {
			// field does not exist
			return;
		}

		long sumTotalTermFreq = terms.getSumTotalTermFreq();
		if (sumTotalTermFreq == -1) {
			throw new IllegalStateException();
		}

		FieldType ft = rb.req.getSchema().getFieldTypeNoEx(field);
		if (ft == null)
			ft = new StrField();

		ContextAccumulator wordToContext1 = getTop(rb.req.getSearcher(), ft, field, prefix1);
		ContextAccumulator wordToContext2 = getTop(rb.req.getSearcher(), ft, field, prefix2);
		ContextAccumulator wordToContext3 = getTop(rb.req.getSearcher(), ft, field, prefix3);
		ContextAccumulator out = new ContextAccumulator(ft, field, null);

		HashSet<String> keySet = new HashSet<String>();
		keySet.addAll(wordToContext1.answer.keySet());
		keySet.addAll(wordToContext2.answer.keySet());
		keySet.addAll(wordToContext3.answer.keySet());

		for (String key : keySet) {
			ValueAndWeight<String> count1 = wordToContext1.answer.get(key);
			ValueAndWeight<String> count2 = wordToContext2.answer.get(key);
			ValueAndWeight<String> count3 = wordToContext3.answer.get(key);

			double value = (count1 == null ? 0d : count1.weight / wordToContext1.totalTermcout)
					- (count2 == null ? 0d : count2.weight / wordToContext2.totalTermcout)
					+ (count3 == null ? 0d : count3.weight / wordToContext3.totalTermcout);
			out.answer.put(key, new ValueAndWeight<String>(key, value));
			out.totalTermcout += value;
		}

		ArrayList<ValueAndWeight<String>> predictions = addPrediction(rb, sumTotalTermFreq, null /*out*/, ft, field);

		ArrayList<ValueAndWeight<String>> candidates2 = new ArrayList<ValueAndWeight<String>>();
		int i = 0;
		for (ValueAndWeight<String> vaw : predictions) {
			if (vaw.weight > 0) {
				candidates2.add(vaw);
				i++;
				if (i >= limit)
					break;
			}
		}

		for (ValueAndWeight<String> vaw : candidates2) {
			ContextAccumulator candidadetVector = getTop(rb.req.getSearcher(), ft, field, vaw.key);
			double toOne = getCosineSimilarity(wordToContext1, candidadetVector);
			double toTwo = getCosineSimilarity(wordToContext2, candidadetVector);
			double toThree = getCosineSimilarity(wordToContext3, candidadetVector);

			vaw.weight = toOne * toThree / (toTwo + 0.001);

		}
		candidates2.sort(null);

		NamedList<Object> isTo = new SimpleOrderedMap<>();
		rb.rsp.add("isTo", isTo);
		for (ValueAndWeight<String> candidate : candidates2) {
			isTo.add(candidate.key, candidate.weight);
		}

	}

	public void predict(ResponseBuilder rb) throws IOException {
		SolrParams params = rb.req.getParams();
		int limit = params.getInt(CONTEXT_LIMIT, 100);

		String[] fields = params.getParams(CONTEXT_FIELD);

		NamedList<Object> termsResult = new SimpleOrderedMap<>();
		rb.rsp.add("terms", termsResult);

		if (fields == null || fields.length == 0)
			return;

		double pmi_power = params.getDouble(CONTEXT_PMI_POWER, 1f);
		double pmi_shift = params.getDouble(CONTEXT_PMI_SHIFT, 0f);
		int pmi_min_tf = params.getInt(CONTEXT_PMI_MIN_TF, 100);
		int pmi_min_cf = params.getInt(CONTEXT_PMI_MIN_CF, 1);
		
		boolean pmi_normalise = params.getBool(CONTEXT_PMI_NORMALISE, false);
		
		String[] prefixes = params.getParams(CONTEXT_PREFIX_STR);

		if (prefixes == null || prefixes.length != 1)
			return;

		String prefix = prefixes[0];

		final LeafReader indexReader = rb.req.getSearcher().getSlowAtomicReader();
		Fields lfields = indexReader.fields();

		for (String field : fields) {
			NamedList<NamedList<Integer>> fieldTerms = new NamedList<>();
			termsResult.add(field, fieldTerms);

			Terms terms = lfields.terms(field);
			if (terms == null) {
				// field does not exist
				continue;
			}

			long sumTotalTermFreq = terms.getSumTotalTermFreq();
			if (sumTotalTermFreq == -1) {
				throw new IllegalStateException();
			}

			FieldType ft = rb.req.getSchema().getFieldTypeNoEx(field);
			if (ft == null)
				ft = new StrField();

			ContextAccumulator2 wordToContext = getTop2(rb.req.getSearcher(), ft, field, prefix, sumTotalTermFreq, pmi_power, pmi_normalise, pmi_shift, pmi_min_tf, pmi_min_cf, limit);

			addPrediction(rb, sumTotalTermFreq, wordToContext, ft, field);

		}

	}

	private ArrayList<ValueAndWeight<String>> addPrediction(ResponseBuilder rb, long sumTotalTermFreq,
			ContextAccumulator2 wordToContext, FieldType ft, String field) {
		SolrParams params = rb.req.getParams();
		int limit = params.getInt(CONTEXT_LIMIT, 100);

		String mode = params.get(CONTEXT_MODE, "pmi");
		boolean pmi = mode.equalsIgnoreCase("pmi");
		double pmi_power = params.getDouble(CONTEXT_PMI_POWER, 1f);
		double pmi_shift = params.getDouble(CONTEXT_PMI_SHIFT, 0f);
		int pmi_min_tf = params.getInt(CONTEXT_PMI_MIN_TF, 100);
		int pmi_min_cf = params.getInt(CONTEXT_PMI_MIN_CF, 1);
		boolean pmi_normalise = params.getBool(CONTEXT_PMI_NORMALISE, false);

		boolean rerank = params.getBool(CONTEXT_RERANK, true);

		HashMap<ValueAndWeight<String>, ValueAndWeight<String>> contextWords = new HashMap<ValueAndWeight<String>, ValueAndWeight<String>>();

		for (ValueAndWeight<String> context : wordToContext.getTop(100)) {
			double pmiToContext = Math.pow(context.weight / sumTotalTermFreq, pmi_power);
			pmiToContext /= (getTermCount(rb.req.getSearcher(), ft, field, wordToContext.prefix)
					/ sumTotalTermFreq)
					* (getTermCount(rb.req.getSearcher(), ft, field, context.key) / sumTotalTermFreq);
			pmiToContext = Math.log(pmiToContext);
			if (pmi_normalise) {
				pmiToContext /= -Math.log(context.weight / sumTotalTermFreq);
			}
			pmiToContext -= pmi_shift;

			if (!pmi || (pmiToContext > 0)) {
				ContextAccumulator2 contextToWord = getTop2(rb.req.getSearcher(), ft, field, context.key, sumTotalTermFreq, pmi_power, pmi_normalise, pmi_shift, pmi_min_tf, pmi_min_cf, limit);
				for (ValueAndWeight<String> word : contextToWord.getTop(100)) {
					String wordKey = word.key;
					if ((wordToContext.prefix != null) && (wordKey.equals(wordToContext.prefix))) {
						wordKey = context.key;
					}

					ValueAndWeight<String> key = new ValueAndWeight<String>(wordKey);
					ValueAndWeight<String> vaw = contextWords.get(key);
					if (vaw == null) {
						vaw = key;
						contextWords.put(key, vaw);
					}
					if (pmi) {
						double pmiFromContext = Math.pow(word.weight / sumTotalTermFreq, pmi_power);
						pmiFromContext /= (getTermCount(rb.req.getSearcher(), ft, field, word.key)
								/ sumTotalTermFreq)
								* (getTermCount(rb.req.getSearcher(), ft, field, context.key)
										/ sumTotalTermFreq);
						pmiFromContext = Math.log(pmiFromContext);
						if (pmi_normalise) {
							pmiFromContext /= -Math.log(word.weight / sumTotalTermFreq);
						}
						pmiFromContext -= pmi_shift;
						pmiFromContext = Math.max(pmiFromContext, 0);

						vaw.weight += pmiToContext * pmiFromContext;
					} else {
						vaw.weight += context.weight * word.weight
								/ getTermCount(rb.req.getSearcher(), ft, field, context.key)
								/ getTermCount(rb.req.getSearcher(), ft, field, wordToContext.prefix);
					}
				}
			}

		}

		ArrayList<ValueAndWeight<String>> candidates = new ArrayList<ValueAndWeight<String>>(contextWords.keySet());
		candidates.sort(null);

		ArrayList<ValueAndWeight<String>> candidates2 = new ArrayList<ValueAndWeight<String>>();
		int i = 0;
		for (ValueAndWeight<String> vaw : candidates) {
			if (vaw.weight > 0) {
				candidates2.add(vaw);
				i++;
				if (i >= limit)
					break;
			}
		}

		for (ValueAndWeight<String> vaw : candidates2) {
			if (rerank) {
				vaw.weight = getCosineSimilarity(wordToContext, getTop2(rb.req.getSearcher(), ft, field, vaw.key, sumTotalTermFreq, pmi_power, pmi_normalise, pmi_shift, pmi_min_tf, pmi_min_cf, limit));
			}
		}
		candidates2.sort(null);

		NamedList<Object> candidatesResult = new SimpleOrderedMap<>();
		rb.rsp.add("candidates", candidatesResult);
		for (ValueAndWeight<String> candidate : candidates2) {
			candidatesResult.add(candidate.key, candidate.weight);
		}

		return candidates;
	}

	private void cosineSimilarity(ResponseBuilder rb) throws IOException {
		NamedList<Object> sim = new SimpleOrderedMap<>();
		rb.rsp.add("sim", sim);

		double cosineSim = getCosineSimilarity(rb, null, null);
		sim.add(" <-> ", "" + cosineSim);

	}

	private double getCosineSimilarity(ResponseBuilder rb, String one, String two) throws IOException {
		SolrParams params = rb.req.getParams();

		int limit = params.getInt(CONTEXT_LIMIT, 100);

		String[] fields = params.getParams(CONTEXT_FIELD);

		if (fields == null || fields.length != 1) {
			return 0.0;
		}
		String field = fields[0];

		String prefix1 = one;
		String prefix2 = two;
		if ((one == null) && (two == null)) {

			String[] prefixes = params.getParams(CONTEXT_PREFIX_STR);
			if (prefixes == null || prefixes.length != 2) {
				return 0.0;
			}

			prefix1 = prefixes[0];
			prefix2 = prefixes[1];
		}

		final LeafReader indexReader = rb.req.getSearcher().getSlowAtomicReader();
		Fields lfields = indexReader.fields();

		Terms terms = lfields.terms(field);
		if (terms == null) {
			// field does not exist
			return 0.0;
		}

		long sumTotalTermFreq = terms.getSumTotalTermFreq();
		if (sumTotalTermFreq == -1) {
			throw new IllegalStateException();
		}

		FieldType ft = rb.req.getSchema().getFieldTypeNoEx(field);
		if (ft == null)
			ft = new StrField();

		ContextAccumulator wordToContext1 = getTop(rb.req.getSearcher(), ft, field, prefix1);
		ContextAccumulator wordToContext2 = getTop(rb.req.getSearcher(), ft, field, prefix2);

		return getCosineSimilarity(wordToContext1, wordToContext2);
	}

	private double getCosineSimilarity(ContextAccumulator wordToContext1, ContextAccumulator wordToContext2) {
		HashSet<String> keySet = new HashSet<String>();
		keySet.addAll(wordToContext1.answer.keySet());
		keySet.addAll(wordToContext2.answer.keySet());

		double sumSq1 = 0.0;
		double sumSq2 = 0.0;
		double sum12 = 0.0;

		for (String key : keySet) {
			ValueAndWeight<String> count1 = wordToContext1.answer.get(key);
			ValueAndWeight<String> count2 = wordToContext2.answer.get(key);

			if (count1 != null) {
				sumSq1 += (count1.weight / wordToContext1.totalTermcout)
						* (count1.weight / wordToContext1.totalTermcout);
			}

			if (count2 != null) {
				sumSq2 += (count2.weight / wordToContext2.totalTermcout)
						* (count2.weight / wordToContext2.totalTermcout);
			}

			if ((count1 != null) && (count2 != null)) {
				sum12 += (count1.weight / wordToContext1.totalTermcout)
						* (count2.weight / wordToContext2.totalTermcout);
			}

		}

		double denom = Math.sqrt(sumSq1) * Math.sqrt(sumSq2);
		if (denom > 0) {
			return sum12 / denom;
		} else {
			return 0.0;
		}
	}
	
	
	private double getCosineSimilarity(ContextAccumulator2 wordToContext1, ContextAccumulator2 wordToContext2) {
		HashSet<String> keySet = new HashSet<String>();
		keySet.addAll(wordToContext1.answer.keySet());
		keySet.addAll(wordToContext2.answer.keySet());

		double sumSq1 = 0.0;
		double sumSq2 = 0.0;
		double sum12 = 0.0;

		for (String key : keySet) {
			ValueAndWeight<String> count1 = wordToContext1.answer.get(key);
			ValueAndWeight<String> count2 = wordToContext2.answer.get(key);

			if (count1 != null) {
				sumSq1 += (count1.weight / wordToContext1.totalTermcout)
						* (count1.weight / wordToContext1.totalTermcout);
			}

			if (count2 != null) {
				sumSq2 += (count2.weight / wordToContext2.totalTermcout)
						* (count2.weight / wordToContext2.totalTermcout);
			}

			if ((count1 != null) && (count2 != null)) {
				sum12 += (count1.weight / wordToContext1.totalTermcout)
						* (count2.weight / wordToContext2.totalTermcout);
			}

		}

		double denom = Math.sqrt(sumSq1) * Math.sqrt(sumSq2);
		if (denom > 0) {
			return sum12 / denom;
		} else {
			return 0.0;
		}
	}

	private ContextAccumulator getTop(IndexSearcher searcher, FieldType ft, String field, String prefix) {
		ContextAccumulator expensive = blackList.get(prefix);
		if (expensive != null) {
			return expensive;
		}

		ContextAccumulator action = new ContextAccumulator(ft, field, prefix);
		long start = System.nanoTime();
		searcher.getIndexReader().leaves().forEach(action);
		long end = System.nanoTime();

		if ((end - start) / 1000000000f > 5) {
			blackList.put(prefix, new ContextAccumulator(ft, field, prefix));
		}

		return action;
	}

	static class ContextAccumulator implements Consumer<LeafReaderContext> {
		double totalTermcout;
		HashMap<String, ValueAndWeight<String>> answer = new HashMap<String, ValueAndWeight<String>>(200000);
		String field;
		String prefix;
		FieldType ft;

		ContextAccumulator(FieldType ft, String field, final String prefix) {
			this.ft = ft;
			this.field = field;
			this.prefix = prefix;
		}

		@Override
		public void accept(LeafReaderContext t) {
			try {
				BytesRef prefixBytes = prefix == null ? null : new BytesRef(prefix + ":");

				Terms terms = t.reader().terms(field);
				if (terms == null) {
					return;
				}
				TermsEnum termsEnum = terms.iterator();
				BytesRef term = null;

				if (termsEnum.seekCeil(prefixBytes) == TermsEnum.SeekStatus.END) {
					termsEnum = null;
				} else {
					term = termsEnum.term();
				}

				while (term != null) {
					// stop if the prefix doesn't match
					if (prefixBytes != null && !StringHelper.startsWith(term, prefixBytes))
						break;

					long freq = termsEnum.totalTermFreq();
					if (freq == -1) {
						throw new IllegalStateException();
					}

					CharsRefBuilder external = new CharsRefBuilder();
					ft.indexedToReadable(term, external);
					String externalString = external.toString().substring(prefix.length() + 1);

					if (!blackList.containsKey(externalString)) {
						if (!externalString.equals("_")) {
							totalTermcout += freq;
							ValueAndWeight<String> vaw = answer.get(externalString);
							if (vaw == null) {
								vaw = new ValueAndWeight<String>(externalString);
								answer.put(externalString, vaw);
							}
							vaw.weight += freq;
						}
					}

					term = termsEnum.next();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		public List<ValueAndWeight<String>> getTop(int limit) {
			ArrayList<ValueAndWeight<String>> candidates = new ArrayList<ValueAndWeight<String>>(answer.values());
			candidates.sort(null);

			ArrayList<ValueAndWeight<String>> candidates2 = new ArrayList<ValueAndWeight<String>>();
			int i = 0;
			for (ValueAndWeight<String> vaw : candidates) {
				if (vaw.weight > 0) {
					candidates2.add(vaw);
					i++;
					if (i >= limit)
						break;
				}
			}
			return candidates2;
		}

	}

	private static ContextAccumulator2 getTop2(IndexSearcher searcher, FieldType ft, String field, String prefix, double sumTotalTermFreq, double pmi_power, boolean pmi_normalise, double pmi_shift, int pmi_min_tf, int pmi_min_cf, int limit) {
		ContextAccumulator2 expensive = cache.get(prefix);
		if (expensive != null) {
			return expensive;
		}

		ContextAccumulator2 action = new ContextAccumulator2(searcher, ft, field, prefix, sumTotalTermFreq, pmi_power, pmi_normalise, pmi_shift, pmi_min_tf, pmi_min_cf);
		searcher.getIndexReader().leaves().forEach(action);
		action.filter();
		action.getTop(limit);
	    cache.put(prefix, action);

		return action;
	}
	
	
	static class ContextAccumulator2 implements Consumer<LeafReaderContext> {
		double totalTermcout;
		HashMap<String, ValueAndWeight<String>> answer = new HashMap<String, ValueAndWeight<String>>(200000);
		String field;
		String prefix;
		FieldType ft;
		IndexSearcher searcher;
		double sumTotalTermFreq;
		double pmi_power;
		boolean pmi_normalise;
		double pmi_shift;
		int pmi_min_tf;
		int pmi_min_cf;

		ContextAccumulator2(IndexSearcher searcher, FieldType ft, String field, final String prefix,
				double sumTotalTermFreq, double pmi_power, boolean pmi_normalise, double pmi_shift, int pmi_min_tf, int pmi_min_cf) {
			this.ft = ft;
			this.field = field;
			this.prefix = prefix;
			this.searcher = searcher;
			this.sumTotalTermFreq = sumTotalTermFreq;
			this.pmi_power = pmi_power;
			this.pmi_normalise = pmi_normalise;
			this.pmi_shift = pmi_shift;
			this.pmi_min_tf = pmi_min_tf;
			this.pmi_min_cf = pmi_min_cf;
		}

		@Override
		public void accept(LeafReaderContext t) {
			try {
				BytesRef prefixBytes = prefix == null ? null : new BytesRef(prefix + ":");

				Terms terms = t.reader().terms(field);
				if (terms == null) {
					return;
				}
				TermsEnum termsEnum = terms.iterator();
				BytesRef term = null;

				if (termsEnum.seekCeil(prefixBytes) == TermsEnum.SeekStatus.END) {
					termsEnum = null;
				} else {
					term = termsEnum.term();
				}

				while (term != null) {
					// stop if the prefix doesn't match
					if (prefixBytes != null && !StringHelper.startsWith(term, prefixBytes))
						break;

					long freq = termsEnum.totalTermFreq();
					if (freq == -1) {
						throw new IllegalStateException();
					}

					CharsRefBuilder external = new CharsRefBuilder();
					ft.indexedToReadable(term, external);
					String externalString = external.toString().substring(prefix.length() + 1);

					totalTermcout += freq;
					ValueAndWeight<String> vaw = answer.get(externalString);
					if (vaw == null) {
						vaw = new ValueAndWeight<String>(externalString);
						answer.put(externalString, vaw);
					}
					vaw.weight += freq;
					

					term = termsEnum.next();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
		public void filter() {
			HashSet<String> toRemove = new HashSet<String>(200000);
			for(String key : answer.keySet())
			{
				ValueAndWeight<String> value = answer.get(key);
				
				double valueTermCount = getTermCount(searcher, ft, field, value.key);
				if((value.weight >= pmi_min_cf) && (valueTermCount >= pmi_min_tf))
				{
					double pmiToContext = Math.pow(value.weight / sumTotalTermFreq, pmi_power);
					pmiToContext /= (getTermCount(searcher, ft, field, prefix) / sumTotalTermFreq)
							* (getTermCount(searcher, ft, field, value.key) / sumTotalTermFreq);
					pmiToContext = Math.log(pmiToContext);
					if (pmi_normalise) {
						pmiToContext /= -Math.log(value.weight / sumTotalTermFreq);
					}
					pmiToContext -= pmi_shift;
					value.pmi = pmiToContext;
					if(pmiToContext < 0)
					{
						toRemove.add(key);
					}
				}
				else
				{
					toRemove.add(key);
				}
				
				
			}
			for(String key : toRemove)
			{
				answer.remove(key);
			}
			
			
		}

		public List<ValueAndWeight<String>> getTop(int limit) {
			ArrayList<ValueAndWeight<String>> candidates = new ArrayList<ValueAndWeight<String>>(answer.values());
			candidates.sort(new Comparator<ValueAndWeight<String>>() {

				@Override
				public int compare(ValueAndWeight<String> one, ValueAndWeight<String> two) {
					return -Double.compare(one.pmi, two.pmi);
				}
			});

			ArrayList<ValueAndWeight<String>> candidates2 = new ArrayList<ValueAndWeight<String>>();
			int i = 0;
			double min = candidates.get(0).pmi;
			for (ValueAndWeight<String> vaw : candidates) {
				if (vaw.weight > 0) {
					candidates2.add(vaw);
					if(vaw.pmi < min)
						min = vaw.pmi;
					i++;
					if (i >= limit)
						break;
				}
			}
			
			HashSet<String> toRemove = new HashSet<String>(200000);
			for(String key : answer.keySet())
			{
				ValueAndWeight<String> value = answer.get(key);
				if(value.pmi < min)
				{
					toRemove.add(key);
				}
			}
			for(String key : toRemove)
			{
				answer.remove(key);
			}
			
			return candidates2;
		}
	}

	private static double getTermCount(IndexSearcher searcher, FieldType ft, String field, String prefix) {
		ContextAccumulator2 counts = getTermCounts(searcher, ft, field, "__tf__");
		ValueAndWeight<String> wav = counts.answer.get(prefix);
		if(wav != null)
		{
			return wav.weight;
		}
		else
		{
			return 0;
		}
	}

	private static synchronized ContextAccumulator2 getTermCounts(IndexSearcher searcher, FieldType ft, String field, String prefix) {
		if(termCounts == null)
		{
			termCounts = new ContextAccumulator2(searcher, ft, field, prefix, 1, 1, false, 1, 1, 1);
			searcher.getIndexReader().leaves().forEach(termCounts);
		}
		return termCounts;
	}
	
	@Override
	public String getDescription() {
		return "A Component for working with Term Enumerators";
	}

	public static class ValueAndWeight<K> implements Comparable<ValueAndWeight<K>> {

		public K key;
		public double weight = 0d;
		public double pmi = 0d;

		public ValueAndWeight(K key) {
			this.key = key;
		}

		public ValueAndWeight(K k, double weight) {
			this.key = k;
			this.weight = weight;
		}
		
		

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((key == null) ? 0 : key.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			@SuppressWarnings("unchecked")
			ValueAndWeight<K> other = (ValueAndWeight<K>) obj;
			if (key == null) {
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key))
				return false;
			return true;
		}

		@Override
		public int compareTo(ValueAndWeight<K> o) {
			return -Double.compare(this.weight, o.weight);
		}

	}
}
