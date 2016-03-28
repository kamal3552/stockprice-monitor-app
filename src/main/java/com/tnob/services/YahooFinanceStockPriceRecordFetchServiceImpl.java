package com.tnob.services;

import com.google.common.collect.Iterables;
import com.tnob.domain.StockPriceHistoryRecord;
import com.tnob.domain.StockRecord;
import com.tnob.repositories.StockPriceRecordRepository;
import com.tnob.repositories.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by tahmid on 3/27/16.
 */

@Component
public class YahooFinanceStockPriceRecordFetchServiceImpl implements StockPriceRecordFetchService {

    private StockRepository stockRepository;
    private StockPriceRecordRepository stockPriceRecordRepository;
    private StockRecordUtilService stockRecordUtilService;

    @Autowired
    public void setStockRepository(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Autowired
    public void setStockPriceRecordRepository(StockPriceRecordRepository stockPriceRecordRepository) {
        this.stockPriceRecordRepository = stockPriceRecordRepository;
    }

    @Autowired
    public void setStockRecordUtilService(StockRecordUtilService stockRecordUtilService) {
        this.stockRecordUtilService = stockRecordUtilService;
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0/30 * * * ?")
    public void fetchAndUpdateStockPriceRecord() throws Exception{

        System.out.println("Fetching from Yahoo API at:" + new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new Date()));

        Iterable <StockRecord> stockRecords = stockRepository.findAll();
        String [] listOfSymbols = stockRecordUtilService.getListOfStockSymbols(stockRecords);
        Map <String, StockRecord> stockRecordSymbolMap = stockRecordUtilService.getStockRecordSymbolMap(stockRecords);

        Map<String, Stock> yahooFinanceStockRecords = YahooFinance.get(listOfSymbols);

        for (String symbol : yahooFinanceStockRecords.keySet()) {
            Stock yahooFinanceStockRecord = yahooFinanceStockRecords.get(symbol);
            BigDecimal lastTradePrice = yahooFinanceStockRecord.getQuote(true).getPrice();
            Date retrievalTime = new Date();
            StockRecord owningStockRecord = stockRecordSymbolMap.get(symbol);

            StockPriceHistoryRecord stockPriceHistoryRecord = new StockPriceHistoryRecord(lastTradePrice,
                    retrievalTime, owningStockRecord);

            stockPriceRecordRepository.save(stockPriceHistoryRecord);

            owningStockRecord.getPriceHistoryRecords().add(stockPriceHistoryRecord);
            stockRepository.save(owningStockRecord);


        }

    }
}
