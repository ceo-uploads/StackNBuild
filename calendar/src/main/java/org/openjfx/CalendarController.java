package org.openjfx;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class CalendarController {

    @FXML private Label titleYear;
    @FXML private Label subtitleDate;
    @FXML private Label monthLabel;
    @FXML private Label currentTime;
    @FXML private VBox calendarVBox;
    @FXML private BorderPane root;
    private VBox dayCell;

    private YearMonth currentYearMonth;

    @FXML
    public void initialize() {
        currentYearMonth = YearMonth.now();
        updateCalendar();
        startClock();
    }

    @FXML
    private void navigateMonth(javafx.event.ActionEvent event) {
        Button clicked = (Button) event.getSource();
        currentYearMonth = clicked.getText().equals("prev")
            ? currentYearMonth.minusMonths(1)
            : currentYearMonth.plusMonths(1);
        updateCalendar();    
    }

    @FXML
    private void closeApplication() {
        System.exit(0);
    }

    private void updateCalendar() {
        calendarVBox.getChildren().removeIf(node -> node instanceof GridPane);

        LocalDate today = LocalDate.now();
        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int monthLength = currentYearMonth.lengthOfMonth();
        int startDayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7;

        titleYear.setText(String.valueOf(currentYearMonth.getYear()));
        subtitleDate.setText(String.format("%02d/%02d/%02d %s", today.getDayOfMonth(), today.getMonthValue(), today.getYear() % 100,
            today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH)));

            monthLabel.setText(currentYearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));

            GridPane calendarGrid = new GridPane();

        calendarGrid.setHgap(10);
        calendarGrid.setVgap(10);

        int col = startDayOfWeek;
        int row = 0;

        for (int day = 1; day <= monthLength; day++) {
            LocalDate date = currentYearMonth.atDay(day);
            dayCell = createDayCell(date, today);
            calendarGrid.add(dayCell, col, row);

            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }

        calendarVBox.getChildren().add(calendarGrid);

    }

    private VBox createDayCell(LocalDate date, LocalDate today) {
        Label dayNumber = new Label(String.valueOf(date.getDayOfMonth()));
        dayNumber.setFont(new Font(18));

        Label dayOfWeek = new Label(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
        dayOfWeek.setFont(new Font(12));

        VBox box = new VBox(5, dayNumber, dayOfWeek);
        box.setPrefSize(80, 80);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.getStyleClass().add("day-cell");

        if (date.equals(today)) {
            box.getStyleClass().add("today-cell");
        }

        return box;
    }

    private void startClock() {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, e-> {
                LocalTime now = LocalTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
                currentTime.setText("Current Time: " + now.format(formatter));
            }),
            new KeyFrame(Duration.seconds(1))
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
    
}
