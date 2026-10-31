package com.careconnect.order.service;

import com.careconnect.clinical.service.VitalService;
import com.careconnect.order.model.LabTest;
import com.careconnect.order.model.OrderEnums.OrderStatus;
import com.careconnect.order.model.OrderEnums.ResultFlag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure-logic rules: lab flagging, BMI and the order lifecycle state machine. */
class ResultFlagTest {

    private static LabTest potassium() {
        LabTest test = new LabTest();
        test.setNormalLow(new BigDecimal("3.5"));
        test.setNormalHigh(new BigDecimal("5.1"));
        test.setCriticalLow(new BigDecimal("2.5"));
        test.setCriticalHigh(new BigDecimal("6.5"));
        return test;
    }

    @ParameterizedTest
    @CsvSource({"2.1,CRITICAL_LOW", "3.0,LOW", "3.5,NORMAL", "4.2,NORMAL", "5.1,NORMAL", "5.8,HIGH", "7.0,CRITICAL_HIGH"})
    void flagsNumericValuesAgainstRanges(String value, ResultFlag expected) {
        assertThat(OrderService.flag(potassium(), new BigDecimal(value), null)).isEqualTo(expected);
    }

    @Test
    void textResultsUseTheAbnormalTickBox() {
        assertThat(OrderService.flag(null, null, true)).isEqualTo(ResultFlag.ABNORMAL);
        assertThat(OrderService.flag(null, null, null)).isEqualTo(ResultFlag.NORMAL);
    }

    @Test
    void bmiIsWeightOverHeightSquared() {
        assertThat(VitalService.calculateBmi(new BigDecimal("78"), new BigDecimal("175"))).isEqualByComparingTo("25.5");
        assertThat(VitalService.calculateBmi(new BigDecimal("78"), null)).isNull();
    }

    @Test
    void orderLifecycleAllowsOnlyForwardMoves() {
        assertThat(OrderStatus.DRAFT.canMoveTo(OrderStatus.PLACED)).isTrue();
        assertThat(OrderStatus.PLACED.canMoveTo(OrderStatus.IN_PROGRESS)).isTrue();
        assertThat(OrderStatus.IN_PROGRESS.canMoveTo(OrderStatus.COMPLETED)).isTrue();
        assertThat(OrderStatus.IN_PROGRESS.canMoveTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.COMPLETED.canMoveTo(OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderStatus.CANCELLED.canMoveTo(OrderStatus.PLACED)).isFalse();
        assertThat(OrderStatus.DRAFT.canMoveTo(OrderStatus.COMPLETED)).isFalse();
    }
}
