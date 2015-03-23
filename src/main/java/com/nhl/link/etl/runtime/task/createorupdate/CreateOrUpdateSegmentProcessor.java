package com.nhl.link.etl.runtime.task.createorupdate;

import java.util.List;

import com.nhl.link.etl.Execution;
import com.nhl.link.etl.TargetListener;

/**
 * A stateless thread-safe processor for batch segments of a create-or-update
 * ETL task.
 * 
 * @since 1.3
 */
public class CreateOrUpdateSegmentProcessor<T> {

	private RowConverter rowConverter;
	private SourceMapper<T> mapper;
	private TargetMatcher<T> matcher;
	private CreateOrUpdateMerger<T> merger;
	private List<TargetListener<T>> loadListeners;

	public CreateOrUpdateSegmentProcessor(RowConverter rowConverter, SourceMapper<T> mapper, TargetMatcher<T> matcher,
			CreateOrUpdateMerger<T> merger, List<TargetListener<T>> loadListeners) {

		this.rowConverter = rowConverter;
		this.mapper = mapper;
		this.matcher = matcher;
		this.merger = merger;
		this.loadListeners = loadListeners;
	}

	public void process(Execution exec, CreateOrUpdateSegment<T> segment) {

		// execute create-or-update pipeline stages
		convertSrc(segment);
		mapSrc(segment);
		matchTarget(segment);
		mergeToTarget(exec, segment);
		commitTarget(segment);
	}

	private void convertSrc(CreateOrUpdateSegment<T> segment) {
		segment.setSources(rowConverter.convert(segment.getSourceRows()));
	}

	private void mapSrc(CreateOrUpdateSegment<T> segment) {
		segment.setMappedSources(mapper.map(segment.getSources()));
	}

	private void matchTarget(CreateOrUpdateSegment<T> segment) {
		segment.setMatchedTargets(matcher.match(segment.getContext(), segment.getMappedSources()));
	}

	private void mergeToTarget(final Execution exec, CreateOrUpdateSegment<T> segment) {
		segment.setMerged(merger.merge(segment.getContext(), segment.getMappedSources(), segment.getMatchedTargets()));

		// dispatch post-merge events
		if (!loadListeners.isEmpty()) {
			for (CreateOrUpdateTuple<T> t : segment.getMerged()) {

				if (t.isCreated()) {
					for (TargetListener<T> l : loadListeners) {
						l.targetCreated(exec, t.getSource(), t.getTarget());
					}
				} else {
					for (TargetListener<T> l : loadListeners) {
						l.targetUpdated(exec, t.getSource(), t.getTarget());
					}
				}
			}
		}
	}

	private void commitTarget(CreateOrUpdateSegment<T> segment) {
		segment.getContext().commitChanges();
	}

}