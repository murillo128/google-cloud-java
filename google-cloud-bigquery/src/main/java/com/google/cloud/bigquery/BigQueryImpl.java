/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.bigquery;

import static com.google.cloud.RetryHelper.runWithRetries;
import static com.google.common.base.Preconditions.checkArgument;

import com.google.api.services.bigquery.model.GetQueryResultsResponse;
import com.google.api.services.bigquery.model.TableDataInsertAllRequest;
import com.google.api.services.bigquery.model.TableDataInsertAllRequest.Rows;
import com.google.api.services.bigquery.model.TableRow;
import com.google.cloud.BaseService;
import com.google.cloud.Page;
import com.google.cloud.PageImpl;
import com.google.cloud.PageImpl.NextPageFetcher;
import com.google.cloud.RetryHelper;
import com.google.cloud.bigquery.InsertAllRequest.RowToInsert;
import com.google.cloud.bigquery.spi.BigQueryRpc;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

final class BigQueryImpl extends BaseService<BigQueryOptions> implements BigQuery {

  private static class DatasetPageFetcher implements NextPageFetcher<Dataset> {

    private static final long serialVersionUID = -3057564042439021278L;
    private final Map<BigQueryRpc.Option, ?> requestOptions;
    private final BigQueryOptions serviceOptions;
    private final String projectId;

    DatasetPageFetcher(String projectId, BigQueryOptions serviceOptions, String cursor,
        Map<BigQueryRpc.Option, ?> optionMap) {
      this.projectId = projectId;
      this.requestOptions =
          PageImpl.nextRequestOptions(BigQueryRpc.Option.PAGE_TOKEN, cursor, optionMap);
      this.serviceOptions = serviceOptions;
    }

    @Override
    public Page<Dataset> nextPage() {
      return listDatasets(projectId, serviceOptions, requestOptions);
    }
  }

  private static class TablePageFetcher implements NextPageFetcher<Table> {

    private static final long serialVersionUID = 8611248840504201187L;
    private final Map<BigQueryRpc.Option, ?> requestOptions;
    private final BigQueryOptions serviceOptions;
    private final DatasetId datasetId;

    TablePageFetcher(DatasetId datasetId, BigQueryOptions serviceOptions, String cursor,
        Map<BigQueryRpc.Option, ?> optionMap) {
      this.requestOptions =
          PageImpl.nextRequestOptions(BigQueryRpc.Option.PAGE_TOKEN, cursor, optionMap);
      this.serviceOptions = serviceOptions;
      this.datasetId = datasetId;
    }

    @Override
    public Page<Table> nextPage() {
      return listTables(datasetId, serviceOptions, requestOptions);
    }
  }

  private static class JobPageFetcher implements NextPageFetcher<Job> {

    private static final long serialVersionUID = 8536533282558245472L;
    private final Map<BigQueryRpc.Option, ?> requestOptions;
    private final BigQueryOptions serviceOptions;

    JobPageFetcher(BigQueryOptions serviceOptions, String cursor,
        Map<BigQueryRpc.Option, ?> optionMap) {
      this.requestOptions =
          PageImpl.nextRequestOptions(BigQueryRpc.Option.PAGE_TOKEN, cursor, optionMap);
      this.serviceOptions = serviceOptions;
    }

    @Override
    public Page<Job> nextPage() {
      return listJobs(serviceOptions, requestOptions);
    }
  }

  private static class TableDataPageFetcher implements NextPageFetcher<List<FieldValue>> {

    private static final long serialVersionUID = -8501991114794410114L;
    private final Map<BigQueryRpc.Option, ?> requestOptions;
    private final BigQueryOptions serviceOptions;
    private final TableId table;

    TableDataPageFetcher(TableId table, BigQueryOptions serviceOptions, String cursor,
        Map<BigQueryRpc.Option, ?> optionMap) {
      this.requestOptions =
          PageImpl.nextRequestOptions(BigQueryRpc.Option.PAGE_TOKEN, cursor, optionMap);
      this.serviceOptions = serviceOptions;
      this.table = table;
    }

    @Override
    public Page<List<FieldValue>> nextPage() {
      return listTableData(table, serviceOptions, requestOptions);
    }
  }

  private static class QueryResultsPageFetcherImpl
      implements NextPageFetcher<List<FieldValue>>, QueryResult.QueryResultsPageFetcher {

    private static final long serialVersionUID = -9198905840550459803L;
    private final Map<BigQueryRpc.Option, ?> requestOptions;
    private final BigQueryOptions serviceOptions;
    private final JobId job;

    QueryResultsPageFetcherImpl(JobId job, BigQueryOptions serviceOptions, String cursor,
        Map<BigQueryRpc.Option, ?> optionMap) {
      this.requestOptions =
          PageImpl.nextRequestOptions(BigQueryRpc.Option.PAGE_TOKEN, cursor, optionMap);
      this.serviceOptions = serviceOptions;
      this.job = job;
    }

    @Override
    public QueryResult nextPage() {
      return getQueryResults(job, serviceOptions, requestOptions).result();
    }
  }

  private final BigQueryRpc bigQueryRpc;

  BigQueryImpl(BigQueryOptions options) {
    super(options);
    bigQueryRpc = options.rpc();
  }

  @Override
  public Dataset create(DatasetInfo datasetInfo, DatasetOption... options) {
    final com.google.api.services.bigquery.model.Dataset datasetPb =
        datasetInfo.setProjectId(options().projectId()).toPb();
    final Map<BigQueryRpc.Option, ?> optionsMap = optionMap(options);
    try {
      return Dataset.fromPb(this,
          runWithRetries(new Callable<com.google.api.services.bigquery.model.Dataset>() {
            @Override
            public com.google.api.services.bigquery.model.Dataset call() {
              return bigQueryRpc.create(datasetPb, optionsMap);
            }
          }, options().retryParams(), EXCEPTION_HANDLER, options().clock()));
    } catch (RetryHelper.RetryHelperException e) {
      throw BigQueryException.translateAndThrow(e);
    }
  }

  @Override
  public Table create(TableInfo tableInfo, TableOption... options) {
    final com.google.api.services.bigquery.model.Table tablePb =
        tableInfo.setProjectId(options().projectId()).toPb();
    final Map<BigQueryRpc.Option, ?> optionsMap = optionMap(options);
    try {
      return Table.fromPb(this,
          runWithRetries(new Callable<com.google.api.services.bigquery.model.Table>() {
            @Override
            public com.google.api.services.bigquery.model.Table call() {
              return bigQueryRpc.create(tablePb, optionsMap);
            }
          }, options().retryParams(), EXCEPTION_HANDLER, options().clock()));
    } catch (RetryHelper.RetryHelperException e) {
      throw BigQueryException.translateAndThrow(e);
    }
  }

  @Override
  public Job create(JobInfo jobInfo, JobOption... options) {
    final com.google.api.services.bigquery.model.Job jobPb =
        jobInfo.setProjectId(options().projectId()).toPb();
    final Map<BigQueryRpc.Option, ?> optionsMap = optionMap(options);
    try {
      return Job.fromPb(this,
          runWithRetries(new Callable<com.google.api.services.bigquery.model.Job>() {
            @Override
            public com.google.api.services.bigquery.model.Job call() {
              return bigQueryRpc.create(jobPb, optionsMap);
            }
          }, options().retryParams(), EXCEPTION_HANDLER, options().clock()));
    } catch (RetryHelper.RetryHelperException e) {
      throw BigQueryException.translateAndThrow(e);
    }
  }

  @Override
  public Dataset getDataset(String datasetId, DatasetOption... options) {
    return getDataset(DatasetId.of(datasetId), options);
  }

  @Override
  public Dataset getDataset(final DatasetId datasetId, DatasetOption... options) {
    final DatasetId completeDatasetId = datasetId.setProjectId(options().projectId());
    final Map<BigQueryRpc.Option, ?> optionsMap = optionMap(options);
    try {
      com.google.api.services.bigquery.model.Dataset answer =
          runWithRetries(new Callable<com.google.api.services.bigquery.model.Dataset>() {
            @Override
            public com.google.api.services.bigquery.model.Dataset call() {
              return bigQueryRpc.getDataset(
                  completeDatasetId.project(), completeDatasetId.dataset(), optionsMap);
            }
          }, options().retryParams(), EXCEPTION_HANDLER, options().clock());
      return answer == null ? null : Dataset.fromPb(this, answer);
    } catch (RetryHelper.RetryHelperException e) {
      throw BigQueryException.translateAndThrow(e);
    }
  }

  @Override
  public Page<Dataset> listDatasets(DatasetListOption... options) {
    return listDatasets(options().projectId(), options);
  }

  @Override
  public Page<Dataset> listDatasets(String projectId, DatasetListOption... options) {
    return listDatasets(projectId, options(), optionMap(options));
  }

  private static Page<Dataset> listDatasets(final String projectId,
      final BigQueryOptions serviceOptions, final Map<BigQueryRpc.Option, ?> optionsMap) {
    try {
      BigQueryRpc.Tuple<String, Iterable<com.google.api.services.bigquery.model.Dataset>> result =
          runWithRetries(new Callable<BigQueryRpc.Tuple<String,
              Iterable<com.google.api.services.bigquery.model.Dataset>>>() {
                @Override
                public BigQueryRpc.Tuple<String,
                    Iterable<com.google.api.services.bigquery.model.Dataset>> call() {
                  return serviceOptions.rpc().listDatasets(projectId, optionsMap);
                }
              }, serviceOptions.retryParams(), EXCEPTION_HANDLER, serviceOptions.clock());
      String cursor = result.x();
      return new PageImpl<>(new DatasetPageFetcher(projectId, serviceOptions, cursor, optionsMap),
          cursor, Iterables.transform(result.y(),
              new Function<com.google.api.services.bigquery.model.Dataset, Dataset>() {
                @Override
                public Dataset apply(com.google.api.services.bigquery.model.Dataset dataset) {
                  return Dataset.fromPb(serviceOptions.service(), dataset);
                }
              }));
    } catch (RetryHelper.RetryHelperException e) {
      throw BigQueryException.translateAndThrow(e);
    }
  }

  @Override
  public boolean delete(String datasetId, DatasetDeleteOption... options) {
    return delete(DatasetId.of(datasetId), options);
  }

  @Override
  public boolean delete(DatasetId datasetId, DatasetDeleteOption... options) {
    final DatasetId completeDatasetId = datasetId.setProjectId(options().projectId());
    final Map<BigQueryRpc.Option, ?> optionsMap = optionMap(options);
    try {
      return runWithRetries(new Callable<Boolean>() {
        @Override
        public Boolean call() {
          return bigQueryRpc.deleteDataset(
              completeDatasetId.project(), completeDatasetId.dataset(), optionsMap);
        }
      }, options().retryParams(), EXCEPTION_HANDLER, options().clock());
    } catch (RetryHelper.RetryHelperException e) {
      throw BigQueryException.translateAndThrow(e);
    }
  }

  @Override
  public boolean delete(String datasetId, String tableId) {
    return delete(TableId.of(datasetId, tableId));
  }

  @Override
  public boolean delete(TableId tableId) {
    final TableId completeTableId = tableId.setProjectId(options().projectId());
    try {
      return runWithRetries(new Callable<Boolean>() {
        @Override
        public Boolean call() {
          return bigQueryRpc.deleteTable(
              completeTableId.project(), completeTableId.dataset(), completeTableId.table());
        }
      }, options().retryParams(), EXCEPTION_HANDLER, options().clock());
    } catch (RetryHelper.RetryHelperException e) {
      throw BigQueryException.translateAndThrow(e);
    }
  }

  @Override
  public Dataset update(DatasetInfo datasetInfo, DatasetOption... options) {
    final com.google.api.services.bigquery.model.Dataset datasetPb =
        datasetInfo.setProjectId(options().projectId()).toPb();
    final Map<BigQueryRpc.Option, ?> optionsMap = optionMap(options);
    try {
      return Dataset.fromPb(this,
          runWithRetries(new Callable<com.google.api.services.bigquery.model.Dataset>() {
            @Override
            public com.google.api.services.bigquery.model.Dataset call() {
              return bigQueryRpc.patch(datasetPb, optionsMap);
            }
          }, options().retryParams(), EXCEPTION_HANDLER, options().clock()));
    } catch (RetryHelper.RetryHelperException e) {
      throw BigQueryException.translateAndThrow(e);
    }
  }

  @Override
  public Table update(TableInfo tableInfo, TableOption... options) {
    final com.google.api.services.bigquery.model.Table tablePb =
        tableInfo.setProjectId(options().projectId()).toPb();
    final Map<BigQueryRpc.Option, ?> optionsMap = optionMap(options);
    try {
      return Table.fromPb(this,
          runWithRetries(new Callable<com.google.api.services.bigquery.model.Table>() {
            @Override
            public com.google.api.services.bigquery.model.Table call() {
              return bigQueryRpc.patch(tablePb, optionsMap);
            }
          }, options().retryParams(), EXCEPTION_HANDLER, options().clock()));
    } catch (RetryHelper.RetryHelperException e) {
      throw BigQueryException.translateAndThrow(e);
    }
  }

  @Override
  public Table getTable(final String datasetId, final String tableId, TableOption... options) {
    return getTable(TableId.of(datasetId, tableId), options);
  }

  @Override
  public Table getTable(TableId tableId, TableOption... options) {
    final TableId completeTableId = tableId.setProjectId(options().projectId());
    final Map<BigQueryRpc.Option, ?> optionsMap = optionMap(options);
    try {
      com.google.api.services.bigquery.model.Table answer =
          runWithRetries(new Callable<com.google.api.services.bigquery.model.Table>() {
            @Override
            public com.google.api.services.bigquery.model.Table call() {
              return bigQueryRpc.getTable(completeTableId.project(), completeTableId.dataset(),
                  completeTableId.table(), optionsMap);
            }
          }, options().retryParams(), EXCEPTION_HANDLER, options().clock());
      return answer == null ? null : Table.fromPb(this, answer);
    } catch (RetryHelper.RetryHelperException e) {
      throw BigQueryException.translateAndThrow(e);
    }
  }

  @Override
  public Page<Table> listTables(String datasetId, TableListOption... options) {
    return listTables(
        DatasetId.of(options().projectId(), datasetId), options(), optionMap(options));
  }

  @Override
  public Page<Table> listTables(DatasetId datasetId, TableListOption... options) {
    DatasetId completeDatasetId = datasetId.setProjectId(options().projectId());
    return listTables(completeDatasetId, options(), optionMap(options));
  }

  private static Page<Table> listTables(final DatasetId datasetId,
      final BigQueryOptions serviceOptions, final Map<BigQueryRpc.Option, ?> optionsMap) {
    try {
      BigQueryRpc.Tuple<String, Iterable<com.google.api.services.bigquery.model.Table>> result =
          runWithRetries(new Callable<BigQueryRpc.Tuple<String,
              Iterable<com.google.api.services.bigquery.model.Table>>>() {
            @Override
            public BigQueryRpc.Tuple<String, Iterable<com.google.api.services.bigquery.model.Table>>
                call() {
                  return serviceOptions.rpc().listTables(
                      datasetId.project(), datasetId.dataset(), optionsMap);
                }
          }, serviceOptions.retryParams(), EXCEPTION_HANDLER, serviceOptions.clock());
      String cursor = result.x();
      Iterable<Table> tables = Iterables.transform(result.y(),
          new Function<com.google.api.services.bigquery.model.Table, Table>() {
            @Override
            public Table apply(com.google.api.services.bigquery.model.Table table) {
              return Table.fromPb(serviceOptions.service(), table);
            }
          });
      return new PageImpl<>(new TablePageFetcher(datasetId, serviceOptions, cursor, optionsMap),
          cursor, tables);
    } catch (RetryHelper.RetryHelperException e) {
      throw BigQueryException.translateAndThrow(e);
    }
  }

  @Override
  public InsertAllResponse insertAll(InsertAllRequest request) {
    final TableId tableId = request.table().setProjectId(options().projectId());
    final TableDataInsertAllRequest requestPb = new TableDataInsertAllRequest();
    requestPb.setIgnoreUnknownValues(request.ignoreUnknownValues());
    requestPb.setSkipInvalidRows(request.skipInvalidRows());
    requestPb.setTemplateSuffix(request.templateSuffix());
    List<Rows> rowsPb = Lists.transform(request.rows(), new Function<RowToInsert, Rows>() {
      @Override
      public Rows apply(RowToInsert rowToInsert) {
        return new Rows().setInsertId(rowToInsert.id()).setJson(rowToInsert.content());
      }
    });
    requestPb.setRows(rowsPb);
    return InsertAllResponse.fromPb(
        bigQueryRpc.insertAll(tableId.project(), tableId.dataset(), tableId.table(), requestPb));
  }

  @Override
  public Page<List<FieldValue>> listTableData(String datasetId, String tableId,
      TableDataListOption... options) {
    return listTableData(TableId.of(datasetId, tableId), options(), optionMap(options));
  }

  @Override
  public Page<List<FieldValue>> listTableData(TableId tableId, TableDataListOption... options) {
    return listTableData(tableId, options(), optionMap(options));
  }

  private static Page<List<FieldValue>> listTableData(final TableId tableId,
      final BigQueryOptions serviceOptions, final Map<BigQueryRpc.Option, ?> optionsMap) {
    try {
      final TableId completeTableId = tableId.setProjectId(serviceOptions.projectId());
      BigQueryRpc.Tuple<String, Iterable<TableRow>> result =
          runWithRetries(new Callable<BigQueryRpc.Tuple<String, Iterable<TableRow>>>() {
            @Override
            public BigQueryRpc.Tuple<String, Iterable<TableRow>> call() {
              return serviceOptions.rpc()
                  .listTableData(completeTableId.project(), completeTableId.dataset(),
                      completeTableId.table(), optionsMap);
            }
          }, serviceOptions.retryParams(), EXCEPTION_HANDLER, serviceOptions.clock());
      String cursor = result.x();
      return new PageImpl<>(new TableDataPageFetcher(tableId, serviceOptions, cursor, optionsMap),
          cursor, transformTableData(result.y()));
    } catch (RetryHelper.RetryHelperException e) {
      throw BigQueryException.translateAndThrow(e);
    }
  }

  private static List<List<FieldValue>> transformTableData(Iterable<TableRow> tableDataPb) {
    return ImmutableList.copyOf(
        Iterables.transform(tableDataPb != null ? tableDataPb : ImmutableList.<TableRow>of(),
            new Function<TableRow, List<FieldValue>>() {
              @Override
              public List<FieldValue> apply(TableRow rowPb) {
                return Lists.transform(rowPb.getF(), FieldValue.FROM_PB_FUNCTION);
              }
            }));
  }

  @Override
  public Job getJob(String jobId, JobOption... options) {
    return getJob(JobId.of(jobId), options);
  }

  @Override
  public Job getJob(JobId jobId, JobOption... options) {
    final Map<BigQueryRpc.Option, ?> optionsMap = optionMap(options);
    final JobId completeJobId = jobId.setProjectId(options().projectId());
    try {
      com.google.api.services.bigquery.model.Job answer =
          runWithRetries(new Callable<com.google.api.services.bigquery.model.Job>() {
            @Override
            public com.google.api.services.bigquery.model.Job call() {
              return bigQueryRpc.getJob(completeJobId.project(), completeJobId.job(), optionsMap);
            }
          }, options().retryParams(), EXCEPTION_HANDLER, options().clock());
      return answer == null ? null : Job.fromPb(this, answer);
    } catch (RetryHelper.RetryHelperException e) {
      throw BigQueryException.translateAndThrow(e);
    }
  }

  @Override
  public Page<Job> listJobs(JobListOption... options) {
    return listJobs(options(), optionMap(options));
  }

  private static Page<Job> listJobs(final BigQueryOptions serviceOptions,
      final Map<BigQueryRpc.Option, ?> optionsMap) {
    BigQueryRpc.Tuple<String, Iterable<com.google.api.services.bigquery.model.Job>> result =
        runWithRetries(new Callable<BigQueryRpc.Tuple<String,
            Iterable<com.google.api.services.bigquery.model.Job>>>() {
          @Override
          public BigQueryRpc.Tuple<String, Iterable<com.google.api.services.bigquery.model.Job>>
              call() {
            return serviceOptions.rpc().listJobs(serviceOptions.projectId(), optionsMap);
          }
        }, serviceOptions.retryParams(), EXCEPTION_HANDLER, serviceOptions.clock());
    String cursor = result.x();
    Iterable<Job> jobs = Iterables.transform(result.y(),
        new Function<com.google.api.services.bigquery.model.Job, Job>() {
          @Override
          public Job apply(com.google.api.services.bigquery.model.Job job) {
            return Job.fromPb(serviceOptions.service(), job);
          }
        });
    return new PageImpl<>(new JobPageFetcher(serviceOptions, cursor, optionsMap), cursor, jobs);
  }

  @Override
  public boolean cancel(String jobId) {
    return cancel(JobId.of(jobId));
  }

  @Override
  public boolean cancel(JobId jobId) {
    final JobId completeJobId = jobId.setProjectId(options().projectId());
    try {
      return runWithRetries(new Callable<Boolean>() {
        @Override
        public Boolean call() {
          return bigQueryRpc.cancel(completeJobId.project(), completeJobId.job());
        }
      }, options().retryParams(), EXCEPTION_HANDLER, options().clock());
    } catch (RetryHelper.RetryHelperException e) {
      throw BigQueryException.translateAndThrow(e);
    }
  }

  @Override
  public QueryResponse query(final QueryRequest request) {
    try {
      com.google.api.services.bigquery.model.QueryResponse results =
          runWithRetries(new Callable<com.google.api.services.bigquery.model.QueryResponse>() {
            @Override
            public com.google.api.services.bigquery.model.QueryResponse call() {
              return bigQueryRpc.query(request.setProjectId(options().projectId()).toPb());
            }
          }, options().retryParams(), EXCEPTION_HANDLER, options().clock());
      QueryResponse.Builder builder = QueryResponse.builder();
      JobId completeJobId = JobId.fromPb(results.getJobReference());
      builder.jobId(completeJobId);
      builder.jobCompleted(results.getJobComplete());
      List<TableRow> rowsPb = results.getRows();
      if (results.getJobComplete()) {
        builder.jobCompleted(true);
        QueryResult.Builder resultBuilder = transformQueryResults(completeJobId, rowsPb,
            results.getPageToken(), options(), ImmutableMap.<BigQueryRpc.Option, Object>of());
        resultBuilder.totalBytesProcessed(results.getTotalBytesProcessed());
        resultBuilder.cacheHit(results.getCacheHit());
        if (results.getSchema() != null) {
          resultBuilder.schema(Schema.fromPb(results.getSchema()));
        }
        if (results.getTotalRows() != null) {
          resultBuilder.totalRows(results.getTotalRows().longValue());
        }
        builder.result(resultBuilder.build());
      }
      if (results.getErrors() != null) {
        builder.executionErrors(
            Lists.transform(results.getErrors(), BigQueryError.FROM_PB_FUNCTION));
      }
      return builder.build();
    } catch (RetryHelper.RetryHelperException e) {
      throw BigQueryException.translateAndThrow(e);
    }
  }

  @Override
  public QueryResponse getQueryResults(JobId jobId, QueryResultsOption... options) {
    Map<BigQueryRpc.Option, ?> optionsMap = optionMap(options);
    return getQueryResults(jobId, options(), optionsMap);
  }

  private static QueryResponse getQueryResults(JobId jobId,
      final BigQueryOptions serviceOptions, final Map<BigQueryRpc.Option, ?> optionsMap) {
    final JobId completeJobId = jobId.setProjectId(serviceOptions.projectId());
    try {
      GetQueryResultsResponse results =
          runWithRetries(new Callable<GetQueryResultsResponse>() {
            @Override
            public GetQueryResultsResponse call() {
              return serviceOptions.rpc().getQueryResults(
                  completeJobId.project(), completeJobId.job(), optionsMap);
            }
          }, serviceOptions.retryParams(), EXCEPTION_HANDLER, serviceOptions.clock());
      QueryResponse.Builder builder = QueryResponse.builder();
      builder.jobId(JobId.fromPb(results.getJobReference()));
      builder.etag(results.getEtag());
      builder.jobCompleted(results.getJobComplete());
      List<TableRow> rowsPb = results.getRows();
      if (results.getJobComplete()) {
        QueryResult.Builder resultBuilder = transformQueryResults(completeJobId, rowsPb,
            results.getPageToken(), serviceOptions, ImmutableMap.<BigQueryRpc.Option, Object>of());
        resultBuilder.totalBytesProcessed(results.getTotalBytesProcessed());
        resultBuilder.cacheHit(results.getCacheHit());
        if (results.getSchema() != null) {
          resultBuilder.schema(Schema.fromPb(results.getSchema()));
        }
        if (results.getTotalRows() != null) {
          resultBuilder.totalRows(results.getTotalRows().longValue());
        }
        builder.result(resultBuilder.build());
      }
      if (results.getErrors() != null) {
        builder.executionErrors(
            Lists.transform(results.getErrors(), BigQueryError.FROM_PB_FUNCTION));
      }
      return builder.build();
    } catch (RetryHelper.RetryHelperException e) {
      throw BigQueryException.translateAndThrow(e);
    }
  }

  private static QueryResult.Builder transformQueryResults(JobId jobId, List<TableRow> rowsPb,
      String cursor, BigQueryOptions serviceOptions, Map<BigQueryRpc.Option, ?> optionsMap) {
    QueryResultsPageFetcherImpl nextPageFetcher =
        new QueryResultsPageFetcherImpl(jobId, serviceOptions, cursor, optionsMap);
    return QueryResult.builder()
        .pageFetcher(nextPageFetcher)
        .cursor(cursor)
        .results(transformTableData(rowsPb));
  }

  @Override
  public TableDataWriteChannel writer(WriteChannelConfiguration writeChannelConfiguration) {
    return new TableDataWriteChannel(options(),
        writeChannelConfiguration.setProjectId(options().projectId()));
  }

  private Map<BigQueryRpc.Option, ?> optionMap(Option... options) {
    Map<BigQueryRpc.Option, Object> optionMap = Maps.newEnumMap(BigQueryRpc.Option.class);
    for (Option option : options) {
      Object prev = optionMap.put(option.rpcOption(), option.value());
      checkArgument(prev == null, "Duplicate option %s", option);
    }
    return optionMap;
  }
}
