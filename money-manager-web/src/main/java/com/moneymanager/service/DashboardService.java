package com.moneymanager.service;

import com.moneymanager.dto.DashboardSnapshot;
import com.moneymanager.dto.MonthlyTrend;
import com.moneymanager.model.Transaction;
import com.moneymanager.repository.IGoalRepo;
import com.moneymanager.repository.ITransactionRepo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardService {

    private final ITransactionRepo txRepo;
    private final IGoalRepo goalRepo;

    public DashboardService(ITransactionRepo txRepo, IGoalRepo goalRepo) {
        this.txRepo   = txRepo;
        this.goalRepo = goalRepo;
    }
    public DashboardSnapshot getSnapshot(long userId) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd   = today.withDayOfMonth(today.lengthOfMonth());
        List<Transaction> monthTxs =
                txRepo.findByUserFiltered(userId, monthStart, monthEnd, null);

        BigDecimal income   = sumByType(monthTxs, "INCOME");
        BigDecimal expenses = sumByType(monthTxs, "EXPENSE");
        BigDecimal net      = income.subtract(expenses);
        Map<String, BigDecimal> breakdown = categoryBreakdown(monthTxs);
        BigDecimal goalSavings = goalRepo.getTotalSavedAmount(userId);
        BigDecimal available   = net.subtract(goalSavings);
        List<MonthlyTrend> trend = buildTrend(userId, today);

        return new DashboardSnapshot(today, income, expenses, net,
                goalSavings, available, breakdown, trend);
    }

    private List<MonthlyTrend> buildTrend(long userId, LocalDate today) {
        LocalDate start = today.minusMonths(5).withDayOfMonth(1);
        LocalDate end   = today.withDayOfMonth(today.lengthOfMonth());

        List<Transaction> txs = txRepo.findByUserFiltered(userId, start, end, null);
        LinkedHashMap<YearMonth, BigDecimal[]> map = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            map.put(YearMonth.now().minusMonths(i),
                    new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }

        for (Transaction tx : txs) {
            YearMonth ym = YearMonth.from(tx.getTxDate());
            BigDecimal[] pair = map.get(ym);
            if (pair == null) continue;
            if ("INCOME".equals(tx.getTxType()))  pair[0] = pair[0].add(tx.getAmount());
            else                                   pair[1] = pair[1].add(tx.getAmount());
        }

        return map.entrySet().stream()
                .map(e -> new MonthlyTrend(
                        e.getKey().getYear(),
                        e.getKey().getMonthValue(),
                        e.getValue()[0],
                        e.getValue()[1]))
                .toList();
    }

    private static BigDecimal sumByType(List<Transaction> txs, String type) {
        return txs.stream()
                .filter(t -> type.equals(t.getTxType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    private static Map<String, BigDecimal> categoryBreakdown(List<Transaction> txs) {
        return txs.stream()
                .filter(t -> "EXPENSE".equals(t.getTxType()))
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO,
                                Transaction::getAmount, BigDecimal::add)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }
}
