package com.nhl.link.etl.runtime.task.createorupdate;

import java.util.List;
import java.util.Map;

import org.apache.cayenne.DataObject;
import org.apache.cayenne.ObjectContext;

import com.nhl.link.etl.CountingRowReader;
import com.nhl.link.etl.Execution;
import com.nhl.link.etl.Row;
import com.nhl.link.etl.RowReader;
import com.nhl.link.etl.batch.BatchProcessor;
import com.nhl.link.etl.batch.BatchRunner;
import com.nhl.link.etl.extractor.Extractor;
import com.nhl.link.etl.runtime.cayenne.ITargetCayenneService;
import com.nhl.link.etl.runtime.extractor.IExtractorService;
import com.nhl.link.etl.runtime.task.BaseTask;
import com.nhl.link.etl.runtime.token.ITokenManager;

/**
 * A task that reads streamed source data and creates/updates records in a
 * target DB.
 * 
 * @since 1.3
 */
public class CreateOrUpdateTask<T extends DataObject> extends BaseTask {

	private String extractorName;
	private int batchSize;
	private ITargetCayenneService targetCayenneService;
	private IExtractorService extractorService;
	private CreateOrUpdateSegmentProcessor<T> processor;

	public CreateOrUpdateTask(String extractorName, int batchSize, ITargetCayenneService targetCayenneService,
			IExtractorService extractorService, ITokenManager tokenManager, CreateOrUpdateSegmentProcessor<T> processor) {

		super(tokenManager);

		this.extractorName = extractorName;
		this.batchSize = batchSize;
		this.targetCayenneService = targetCayenneService;
		this.extractorService = extractorService;
		this.processor = processor;
	}

	@Override
	public Execution run(Map<String, ?> params) {

		if (params == null) {
			throw new NullPointerException("Null params");
		}

		try (Execution execution = new Execution("CreateOrUpdateTask:" + extractorName, params);) {

			BatchProcessor<Row> batchProcessor = createBatchProcessor(execution);

			try (RowReader data = getRowReader(execution, params)) {
				BatchRunner.create(batchProcessor).withBatchSize(batchSize).run(data);
			}

			return execution;
		}
	}

	protected BatchProcessor<Row> createBatchProcessor(final Execution execution) {
		return new BatchProcessor<Row>() {

			ObjectContext context = targetCayenneService.newContext();

			@Override
			public void process(List<Row> rows) {
				processor.process(execution, new CreateOrUpdateSegment<T>(context, rows));
			}
		};
	}

	/**
	 * Returns a RowReader obtained from a named extractor and wrapped in a read
	 * stats counter.
	 */
	protected RowReader getRowReader(Execution execution, Map<String, ?> extractorParams) {
		Extractor extractor = extractorService.getExtractor(extractorName);
		return new CountingRowReader(extractor.getReader(extractorParams), execution.getStats());
	}

}
