package com.moneymanager.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final NumberFormat currency = NumberFormat.getCurrencyInstance();
    private final int[] screenIds = {
            R.id.screen_dashboard, R.id.screen_transactions, R.id.screen_budgets,
            R.id.screen_goals, R.id.screen_notes
    };
    private final int[] navigationIds = {
            R.id.nav_dashboard, R.id.nav_transactions, R.id.nav_budgets,
            R.id.nav_goals, R.id.nav_notes
    };

    private MoneyDatabaseHelper database;
    private String monthPrefix;
    private TextView incomeValue;
    private TextView expenseValue;
    private TextView balanceValue;
    private TextView monthTitle;
    private LinearLayout recentTransactions;
    private LinearLayout dashboardBudgets;
    private LinearLayout transactionList;
    private LinearLayout budgetList;
    private LinearLayout goalList;
    private LinearLayout noteList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dateFormat.setLenient(false);
        monthPrefix = new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date());
        database = new MoneyDatabaseHelper(this);

        bindViews();
        bindActions();
        showScreen(0);
        refreshAll();
    }

    private void bindViews() {
        incomeValue = findViewById(R.id.income_value);
        expenseValue = findViewById(R.id.expense_value);
        balanceValue = findViewById(R.id.balance_value);
        monthTitle = findViewById(R.id.month_title);
        recentTransactions = findViewById(R.id.recent_transactions);
        dashboardBudgets = findViewById(R.id.dashboard_budgets);
        transactionList = findViewById(R.id.transaction_list);
        budgetList = findViewById(R.id.budget_list);
        goalList = findViewById(R.id.goal_list);
        noteList = findViewById(R.id.note_list);
    }

    private void bindActions() {
        for (int index = 0; index < navigationIds.length; index++) {
            final int selectedIndex = index;
            findViewById(navigationIds[index]).setOnClickListener(view -> showScreen(selectedIndex));
        }
        findViewById(R.id.add_transaction).setOnClickListener(view -> showTransactionDialog(null));
        findViewById(R.id.add_budget).setOnClickListener(view -> showBudgetDialog(null));
        findViewById(R.id.add_goal).setOnClickListener(view -> showGoalDialog(null));
        findViewById(R.id.add_note).setOnClickListener(view -> showNoteDialog(null));
    }

    private void showScreen(int selectedIndex) {
        for (int index = 0; index < screenIds.length; index++) {
            findViewById(screenIds[index]).setVisibility(index == selectedIndex ? View.VISIBLE : View.GONE);
            Button navigation = findViewById(navigationIds[index]);
            navigation.setTextColor(index == selectedIndex ? Color.WHITE : getColor(R.color.primary));
            navigation.setBackgroundTintList(ColorStateList.valueOf(getColor(
                    index == selectedIndex ? R.color.primary : R.color.nav_unselected)));
        }
    }

    private void refreshAll() {
        MoneyDatabaseHelper.Summary summary = database.getMonthlySummary(monthPrefix);
        monthTitle.setText(getString(R.string.month_summary, monthPrefix));
        incomeValue.setText(formatMoney(summary.incomeCents));
        expenseValue.setText(formatMoney(summary.expenseCents));
        balanceValue.setText(formatMoney(summary.incomeCents - summary.expenseCents));
        balanceValue.setTextColor(getColor(summary.incomeCents - summary.expenseCents >= 0
                ? R.color.income : R.color.expense));

        renderTransactions(transactionList, database.getTransactions(null, 0), true);
        renderTransactions(recentTransactions, database.getTransactions(null, 4), false);
        renderBudgets(budgetList, database.getBudgets(monthPrefix), true);
        renderBudgets(dashboardBudgets, database.getBudgets(monthPrefix), false);
        renderGoals();
        renderNotes();
    }

    private void renderTransactions(LinearLayout target,
                                    List<MoneyDatabaseHelper.TransactionRecord> records,
                                    boolean actionsEnabled) {
        target.removeAllViews();
        if (records.isEmpty()) {
            target.addView(emptyMessage(R.string.empty_transactions));
            return;
        }
        for (MoneyDatabaseHelper.TransactionRecord record : records) {
            LinearLayout card = card();
            TextView title = title(record.name);
            TextView amount = title(typeSign(record.type) + formatMoney(record.amountCents));
            amount.setTextColor(getColor("EXPENSE".equals(record.type) ? R.color.expense : R.color.income));
            card.addView(twoColumns(title, amount));
            card.addView(detail(record.category + "  |  " + record.occurredOn + "  |  " + record.type));
            if (actionsEnabled) {
                card.addView(actionRow(
                        action("Edit", view -> showTransactionDialog(record)),
                        action("Delete", view -> confirmDelete("Delete transaction?", () -> {
                            database.deleteTransaction(record.id);
                            refreshAll();
                        }))));
            }
            target.addView(card);
        }
    }

    private String typeSign(String type) {
        return "EXPENSE".equals(type) ? "-" : "+";
    }

    private void renderBudgets(LinearLayout target, List<MoneyDatabaseHelper.BudgetRecord> records,
                               boolean actionsEnabled) {
        target.removeAllViews();
        if (records.isEmpty()) {
            target.addView(emptyMessage(R.string.empty_budgets));
            return;
        }
        for (MoneyDatabaseHelper.BudgetRecord record : records) {
            LinearLayout card = card();
            card.addView(twoColumns(title(record.category), title(formatMoney(record.spentCents)
                    + " / " + formatMoney(record.limitCents))));
            int percentage = record.limitCents == 0 ? 0
                    : (int) Math.min(100, (record.spentCents * 100) / record.limitCents);
            ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progress.setMax(100);
            progress.setProgress(percentage);
            progress.setProgressTintList(ColorStateList.valueOf(getColor(
                    percentage >= 100 ? R.color.expense : percentage >= 80 ? R.color.warning : R.color.primary)));
            LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(8));
            progressParams.topMargin = dp(10);
            card.addView(progress, progressParams);
            card.addView(detail(percentage + "% used this month"));
            if (actionsEnabled) {
                card.addView(actionRow(
                        action("Edit", view -> showBudgetDialog(record)),
                        action("Delete", view -> confirmDelete("Delete budget?", () -> {
                            database.deleteBudget(record.id);
                            refreshAll();
                        }))));
            }
            target.addView(card);
        }
    }

    private void renderGoals() {
        goalList.removeAllViews();
        List<MoneyDatabaseHelper.GoalRecord> records = database.getGoals();
        if (records.isEmpty()) {
            goalList.addView(emptyMessage(R.string.empty_goals));
            return;
        }
        for (MoneyDatabaseHelper.GoalRecord record : records) {
            LinearLayout card = card();
            card.addView(twoColumns(title(record.name), title(formatMoney(record.savedCents)
                    + " / " + formatMoney(record.targetCents))));
            int percentage = (int) Math.min(100, (record.savedCents * 100) / record.targetCents);
            ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progress.setMax(100);
            progress.setProgress(percentage);
            progress.setProgressTintList(ColorStateList.valueOf(getColor(R.color.income)));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(8));
            params.topMargin = dp(10);
            card.addView(progress, params);
            String deadline = record.deadline == null || record.deadline.isEmpty()
                    ? "No deadline" : "Deadline: " + record.deadline;
            card.addView(detail(percentage + "% complete  |  " + deadline));
            card.addView(actionRow(
                    action("Contribute", view -> showContributionDialog(record)),
                    action("Edit", view -> showGoalDialog(record)),
                    action("Delete", view -> confirmDelete("Delete savings goal?", () -> {
                        database.deleteGoal(record.id);
                        refreshAll();
                    }))));
            goalList.addView(card);
        }
    }

    private void renderNotes() {
        noteList.removeAllViews();
        List<MoneyDatabaseHelper.NoteRecord> records = database.getNotes();
        if (records.isEmpty()) {
            noteList.addView(emptyMessage(R.string.empty_notes));
            return;
        }
        for (MoneyDatabaseHelper.NoteRecord record : records) {
            LinearLayout card = card();
            card.addView(title(record.title));
            TextView content = detail(record.content);
            content.setMaxLines(4);
            card.addView(content);
            card.addView(actionRow(
                    action("Edit", view -> showNoteDialog(record)),
                    action("Delete", view -> confirmDelete("Delete note?", () -> {
                        database.deleteNote(record.id);
                        refreshAll();
                    }))));
            noteList.addView(card);
        }
    }

    private void showTransactionDialog(MoneyDatabaseHelper.TransactionRecord record) {
        LinearLayout form = form();
        EditText name = input("Name", record == null ? "" : record.name, InputType.TYPE_CLASS_TEXT);
        EditText amount = input("Amount", record == null ? "" : centsForInput(record.amountCents),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText category = input("Category", record == null ? "" : record.category, InputType.TYPE_CLASS_TEXT);
        Spinner type = spinner(Arrays.asList("EXPENSE", "INCOME"));
        if (record != null && "INCOME".equals(record.type)) {
            type.setSelection(1);
        }
        EditText date = input("Date (YYYY-MM-DD)", record == null ? today() : record.occurredOn,
                InputType.TYPE_CLASS_DATETIME);
        addFields(form, name, amount, category, type, date);
        AlertDialog dialog = dialog(record == null ? "Add transaction" : "Edit transaction", form);
        dialog.setOnShowListener(ignored -> dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    Long cents = validatedAmount(amount);
                    if (isBlank(name) || isBlank(category) || cents == null || !validDate(date.getText().toString())) {
                        showError("Enter a name, category, positive amount, and valid YYYY-MM-DD date.");
                        return;
                    }
                    database.saveTransaction(new MoneyDatabaseHelper.TransactionRecord(
                            record == null ? 0 : record.id, text(name), cents, text(category),
                            type.getSelectedItem().toString(), text(date)));
                    dialog.dismiss();
                    refreshAll();
                }));
        dialog.show();
    }

    private void showBudgetDialog(MoneyDatabaseHelper.BudgetRecord record) {
        LinearLayout form = form();
        EditText category = input("Expense category", record == null ? "" : record.category,
                InputType.TYPE_CLASS_TEXT);
        EditText limit = input("Monthly limit", record == null ? "" : centsForInput(record.limitCents),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        addFields(form, category, limit);
        AlertDialog dialog = dialog(record == null ? "Add monthly budget" : "Edit monthly budget", form);
        dialog.setOnShowListener(ignored -> dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    Long cents = validatedAmount(limit);
                    if (isBlank(category) || cents == null) {
                        showError("Enter a category and positive monthly limit.");
                        return;
                    }
                    try {
                        database.saveBudget(new MoneyDatabaseHelper.BudgetRecord(
                                record == null ? 0 : record.id, text(category), cents, 0));
                    } catch (SQLiteConstraintException exception) {
                        showError("A budget already exists for that category.");
                        return;
                    }
                    dialog.dismiss();
                    refreshAll();
                }));
        dialog.show();
    }

    private void showGoalDialog(MoneyDatabaseHelper.GoalRecord record) {
        LinearLayout form = form();
        EditText name = input("Goal name", record == null ? "" : record.name, InputType.TYPE_CLASS_TEXT);
        EditText target = input("Target amount", record == null ? "" : centsForInput(record.targetCents),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText deadline = input("Deadline (YYYY-MM-DD, optional)",
                record == null || record.deadline == null ? "" : record.deadline, InputType.TYPE_CLASS_DATETIME);
        addFields(form, name, target, deadline);
        AlertDialog dialog = dialog(record == null ? "Add savings goal" : "Edit savings goal", form);
        dialog.setOnShowListener(ignored -> dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    Long cents = validatedAmount(target);
                    if (isBlank(name) || cents == null ||
                            (!isBlank(deadline) && !validDate(text(deadline)))) {
                        showError("Enter a name, positive target, and optional valid deadline.");
                        return;
                    }
                    database.saveGoal(new MoneyDatabaseHelper.GoalRecord(
                            record == null ? 0 : record.id, text(name), cents,
                            record == null ? 0 : record.savedCents, text(deadline)));
                    dialog.dismiss();
                    refreshAll();
                }));
        dialog.show();
    }

    private void showContributionDialog(MoneyDatabaseHelper.GoalRecord record) {
        LinearLayout form = form();
        EditText amount = input("Contribution amount", "",
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        addFields(form, amount);
        AlertDialog dialog = dialog("Contribute to " + record.name, form);
        dialog.setOnShowListener(ignored -> dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    Long cents = validatedAmount(amount);
                    if (cents == null) {
                        showError("Enter a positive contribution amount.");
                        return;
                    }
                    database.addGoalContribution(record.id, cents);
                    dialog.dismiss();
                    refreshAll();
                }));
        dialog.show();
    }

    private void showNoteDialog(MoneyDatabaseHelper.NoteRecord record) {
        LinearLayout form = form();
        EditText title = input("Title", record == null ? "" : record.title, InputType.TYPE_CLASS_TEXT);
        EditText content = input("Note", record == null ? "" : record.content,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        content.setMinLines(4);
        content.setGravity(Gravity.TOP);
        addFields(form, title, content);
        AlertDialog dialog = dialog(record == null ? "Add note" : "Edit note", form);
        dialog.setOnShowListener(ignored -> dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    if (isBlank(title) || isBlank(content)) {
                        showError("Enter a title and note text.");
                        return;
                    }
                    database.saveNote(new MoneyDatabaseHelper.NoteRecord(
                            record == null ? 0 : record.id, text(title), text(content), null));
                    dialog.dismiss();
                    refreshAll();
                }));
        dialog.show();
    }

    private AlertDialog dialog(String title, View form) {
        return new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(form)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();
    }

    private void confirmDelete(String prompt, Runnable deleteAction) {
        new AlertDialog.Builder(this)
                .setMessage(prompt)
                .setPositiveButton("Delete", (dialog, selection) -> deleteAction.run())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private LinearLayout form() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(22), dp(8), dp(22), 0);
        return form;
    }

    private void addFields(LinearLayout form, View... fields) {
        for (View field : fields) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = dp(8);
            form.addView(field, params);
        }
    }

    private EditText input(String hint, String value, int inputType) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setText(value);
        editText.setInputType(inputType);
        editText.setSelectAllOnFocus(true);
        return editText;
    }

    private Spinner spinner(List<String> choices) {
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, choices));
        return spinner;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundResource(R.drawable.card_background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(10);
        card.setLayoutParams(params);
        return card;
    }

    private LinearLayout twoColumns(View start, View end) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(start, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(end);
        return row;
    }

    private LinearLayout actionRow(Button... buttons) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(8);
        for (Button button : buttons) {
            row.addView(button, params);
        }
        return row;
    }

    private Button action(String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(12);
        button.setTextColor(getColor(R.color.primary));
        button.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.nav_unselected)));
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setOnClickListener(listener);
        return button;
    }

    private TextView title(String value) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(15);
        textView.setTextColor(getColor(R.color.text_primary));
        textView.setTypeface(null, android.graphics.Typeface.BOLD);
        return textView;
    }

    private TextView detail(String value) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(13);
        textView.setTextColor(getColor(R.color.text_secondary));
        textView.setPadding(0, dp(6), 0, 0);
        return textView;
    }

    private TextView emptyMessage(int stringId) {
        TextView textView = detail(getString(stringId));
        textView.setPadding(0, dp(18), 0, dp(8));
        return textView;
    }

    private Long validatedAmount(EditText field) {
        try {
            BigDecimal value = new BigDecimal(text(field)).setScale(2, RoundingMode.HALF_UP);
            if (value.signum() <= 0) {
                return null;
            }
            return value.movePointRight(2).longValueExact();
        } catch (NumberFormatException | ArithmeticException exception) {
            return null;
        }
    }

    private boolean validDate(String value) {
        try {
            dateFormat.parse(value);
            return true;
        } catch (ParseException exception) {
            return false;
        }
    }

    private boolean isBlank(EditText field) {
        return text(field).isEmpty();
    }

    private String text(EditText field) {
        return field.getText().toString().trim();
    }

    private String today() {
        return dateFormat.format(new Date());
    }

    private String formatMoney(long cents) {
        return currency.format(BigDecimal.valueOf(cents, 2));
    }

    private String centsForInput(long cents) {
        return BigDecimal.valueOf(cents, 2).toPlainString();
    }

    private int dp(int size) {
        return Math.round(size * getResources().getDisplayMetrics().density);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }
}
