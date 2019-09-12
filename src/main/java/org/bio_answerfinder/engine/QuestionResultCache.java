package org.bio_answerfinder.engine;


import java.util.List;

import org.bio_answerfinder.util.GenUtils;
import org.bio_answerfinder.util.LRUCache;

public class QuestionResultCache {
	private LRUCache<String, List<AnswerSentence>> cache = new LRUCache<String, List<AnswerSentence>>(200); 
	private static QuestionResultCache instance = null;
	
	public synchronized static QuestionResultCache getInstance() {
		if (instance == null) {
			instance = new QuestionResultCache();
		}
		return instance;
	}
	
	private QuestionResultCache() {
		super();
	}
	
	public synchronized List<AnswerSentence> getAnswers(String question) {
		if (GenUtils.isEmpty(question)) {
			return null;
		}
		return cache.get(question.toLowerCase());
	}
	
	
	public synchronized void cacheAnswers(String question, List<AnswerSentence> answers) {
		cache.put(question.toLowerCase(), answers);
	}
	
	public synchronized void reset() {
		cache.clear();
	}
}
