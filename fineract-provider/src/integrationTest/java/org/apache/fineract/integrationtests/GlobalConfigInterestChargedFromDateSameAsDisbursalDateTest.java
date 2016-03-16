/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.integrationtests;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.GlobalConfigurationHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.accounting.Account;
import org.apache.fineract.integrationtests.common.loans.LoanApplicationTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanProductTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanStatusChecker;
import org.apache.fineract.integrationtests.common.loans.LoanTransactionHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;


public class GlobalConfigInterestChargedFromDateSameAsDisbursalDateTest {
    
    private ResponseSpecification responseSpec;
    private RequestSpecification requestSpec;
    private GlobalConfigurationHelper globalConfigurationHelper;
    private LoanTransactionHelper loanTransactionHelper;
    
    @Before
    public void setup() {
        Utils.initializeRESTAssured();
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
        this.loanTransactionHelper = new LoanTransactionHelper(this.requestSpec, this.responseSpec);
    }
    
    @SuppressWarnings( {"static-access", "rawtypes", "unchecked"})
    @Test
    public void testInterestChargedFromDateSameAsDisbursalDate(){
        this.globalConfigurationHelper = new GlobalConfigurationHelper(this.requestSpec, this.responseSpec);
        
        // Retrieving All Global Configuration details
        final ArrayList<HashMap> globalConfig = this.globalConfigurationHelper
                        .getAllGlobalConfigurations(this.requestSpec, this.responseSpec);
        Assert.assertNotNull(globalConfig);
        
        String configName = "interest-charged-from-date-same-as-disbursal-date";
        boolean newBooleanValue = true;
        
        for (Integer configIndex = 0; configIndex < (globalConfig.size()); configIndex++) {
                if (globalConfig.get(configIndex).get("name")
                                .equals(configName)) {
                        String configId = (globalConfig.get(configIndex).get("id"))
                                        .toString();
                        Integer updateConfigId = this.globalConfigurationHelper
                                        .updateEnabledFlagForGlobalConfiguration(this.requestSpec, this.responseSpec,
                                                configId.toString(), newBooleanValue);;
                        Assert.assertNotNull(updateConfigId);
                        break;
                }
        }
        
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testloanApplication(){
        
        final String disbursementDate = "01 March 2015";
        final String recalculationRestFrequencyDate = "01 January 2012";
        final String approveDate = "01 March 2015";

        final Integer clientID = ClientHelper.createClient(this.requestSpec, this.responseSpec, "01 January 2012");
        
        // CREATE LOAN PRODUCT WITH INTEREST RECALCULATION 
        final Integer loanProductID = createLoanProductWithInterestRecalculation(LoanProductTestBuilder.RBI_INDIA_STRATEGY,
                LoanProductTestBuilder.RECALCULATION_COMPOUNDING_METHOD_NONE,
                LoanProductTestBuilder.RECALCULATION_STRATEGY_REDUCE_NUMBER_OF_INSTALLMENTS,
                LoanProductTestBuilder.RECALCULATION_FREQUENCY_TYPE_DAILY, "0", recalculationRestFrequencyDate,
                LoanProductTestBuilder.INTEREST_APPLICABLE_STRATEGY_ON_PRE_CLOSE_DATE, null);
        
        final Integer loanID = applyForLoanApplicationForInterestRecalculation(clientID, loanProductID, disbursementDate,
                recalculationRestFrequencyDate, LoanApplicationTestBuilder.RBI_INDIA_STRATEGY, new ArrayList<HashMap>(0));
        
        HashMap loanStatusHashMap = LoanStatusChecker.getStatusOfLoan(this.requestSpec, this.responseSpec, loanID);
        LoanStatusChecker.verifyLoanIsPending(loanStatusHashMap);
        
        // VALIDATE THE LOAN STATUS
        LoanStatusChecker.verifyLoanIsPending(loanStatusHashMap);

        System.out.println("-----------------------------------APPROVE LOAN-----------------------------------------------------------");
        loanStatusHashMap = this.loanTransactionHelper.approveLoan(approveDate, loanID);
        
        // VALIDATE THE LOAN IS APPROVED
        LoanStatusChecker.verifyLoanIsApproved(loanStatusHashMap);
        LoanStatusChecker.verifyLoanIsWaitingForDisbursal(loanStatusHashMap);
        
        // DISBURSE A LOAN
        this.loanTransactionHelper.disburseLoan(disbursementDate, loanID);
        loanStatusHashMap = LoanStatusChecker.getStatusOfLoan(this.requestSpec, this.responseSpec, loanID);

        // VALIDATE THE LOAN IS ACTIVE STATUS
        LoanStatusChecker.verifyLoanIsActive(loanStatusHashMap);
        
        final ArrayList<HashMap> loanSchedule = this.loanTransactionHelper.getLoanRepaymentSchedule(this.requestSpec, this.responseSpec,
                loanID);
        // VALIDATE LOAN REPAYMENT SCHEDULE
        verifyLoanRepaymentSchedule(loanSchedule);

        
    }
    
    private void verifyLoanRepaymentSchedule(ArrayList<HashMap> loanSchedule) {
        HashMap firstInstallement  = loanSchedule.get(1);
        Map<String, Object> expectedvalues = new HashMap<>(3);
        Calendar date = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        date.set(2015, Calendar.MARCH, 24);
        expectedvalues.put("dueDate", getDateAsArray(date, 0));
        expectedvalues.put("interestDue", "151.23");
        
        verifyLoanRepaymentSchedule(firstInstallement, expectedvalues);
        
    }
    
    private void verifyLoanRepaymentSchedule(final HashMap firstInstallement, final Map<String, Object> expectedvalues) {
        
        assertEquals(expectedvalues.get("dueDate"), firstInstallement.get("dueDate"));
        assertEquals(String.valueOf(expectedvalues.get("interestDue")), String.valueOf(firstInstallement.get("interestDue")));
}
    
    private Integer applyForLoanApplicationForInterestRecalculation(final Integer clientID, final Integer loanProductID,
            final String disbursementDate, final String restStartDate, final String repaymentStrategy, final List<HashMap> charges) {
        final String graceOnInterestPayment = null;
        final String compoundingStartDate = null;
        final String graceOnPrincipalPayment = null;
        return applyForLoanApplicationForInterestRecalculation(clientID, loanProductID, disbursementDate, restStartDate,
                compoundingStartDate, repaymentStrategy, charges, graceOnInterestPayment, graceOnPrincipalPayment);
    }
    
    private Integer applyForLoanApplicationForInterestRecalculation(final Integer clientID, final Integer loanProductID,
            final String disbursementDate, final String restStartDate, final String compoundingStartDate, final String repaymentStrategy,
            final List<HashMap> charges, final String graceOnInterestPayment, final String graceOnPrincipalPayment) {
        System.out.println("--------------------------------APPLYING FOR LOAN APPLICATION--------------------------------");
        final String loanApplicationJSON = new LoanApplicationTestBuilder() //
                .withPrincipal("10000.00") //
                .withLoanTermFrequency("24") //
                .withLoanTermFrequencyAsWeeks() //
                .withNumberOfRepayments("12") //
                .withRepaymentEveryAfter("2") //
                .withRepaymentFrequencyTypeAsWeeks() //
                .withInterestRatePerPeriod("2") //
                .withAmortizationTypeAsEqualInstallments() //
                .withFixedEmiAmount("") //
                .withFirstRepaymentDate("24 March 2015")
                .withInterestTypeAsDecliningBalance() //
                .withInterestCalculationPeriodTypeAsDays() //
                .withInterestCalculationPeriodTypeAsDays() //
                .withExpectedDisbursementDate(disbursementDate) //
                .withSubmittedOnDate(disbursementDate) //
                .withRestFrequencyDate(restStartDate)//
                .withwithRepaymentStrategy(repaymentStrategy) //
                .withCharges(charges)//
                .build(clientID.toString(), loanProductID.toString(), null);
        return this.loanTransactionHelper.getLoanId(loanApplicationJSON);
    }

    private Integer createLoanProductWithInterestRecalculation(final String repaymentStrategy,
            final String interestRecalculationCompoundingMethod, final String rescheduleStrategyMethod,
            final String recalculationRestFrequencyType, final String recalculationRestFrequencyInterval,
            final String recalculationRestFrequencyDate, final String preCloseInterestCalculationStrategy, final Account[] accounts) {
        final String recalculationCompoundingFrequencyType = null;
        final String recalculationCompoundingFrequencyInterval = null;
        final String recalculationCompoundingFrequencyDate = null;
        return createLoanProductWithInterestRecalculation(repaymentStrategy, interestRecalculationCompoundingMethod,
                rescheduleStrategyMethod, recalculationRestFrequencyType, recalculationRestFrequencyInterval,
                recalculationRestFrequencyDate, recalculationCompoundingFrequencyType, recalculationCompoundingFrequencyInterval,
                recalculationCompoundingFrequencyDate, preCloseInterestCalculationStrategy, accounts, null, false);
    }
    
    private Integer createLoanProductWithInterestRecalculation(final String repaymentStrategy,
            final String interestRecalculationCompoundingMethod, final String rescheduleStrategyMethod,
            final String recalculationRestFrequencyType, final String recalculationRestFrequencyInterval,
            final String recalculationRestFrequencyDate, final String recalculationCompoundingFrequencyType,
            final String recalculationCompoundingFrequencyInterval, final String recalculationCompoundingFrequencyDate,
            final String preCloseInterestCalculationStrategy, final Account[] accounts, final String chargeId,
            boolean isArrearsBasedOnOriginalSchedule) {
        System.out.println("------------------------------CREATING NEW LOAN PRODUCT ---------------------------------------");
        LoanProductTestBuilder builder = new LoanProductTestBuilder()
                .withPrincipal("10000.00")
                .withNumberOfRepayments("12")
                .withRepaymentAfterEvery("2")
                .withRepaymentTypeAsWeek()
                .withinterestRatePerPeriod("2")
                .withInterestRateFrequencyTypeAsMonths()
                .withInterestCalculationPeriodTypeAsRepaymentPeriod(true)
                .withRepaymentStrategy(repaymentStrategy)
                .withInterestTypeAsDecliningBalance()
                .withInterestRecalculationDetails(interestRecalculationCompoundingMethod, rescheduleStrategyMethod,
                        preCloseInterestCalculationStrategy)
                .withInterestRecalculationRestFrequencyDetails(recalculationRestFrequencyType, recalculationRestFrequencyInterval,
                        recalculationRestFrequencyDate)
                .withInterestRecalculationCompoundingFrequencyDetails(recalculationCompoundingFrequencyType,
                        recalculationCompoundingFrequencyInterval, recalculationCompoundingFrequencyDate);
        if (accounts != null) {
            builder = builder.withAccountingRulePeriodicAccrual(accounts);
        }

        if (isArrearsBasedOnOriginalSchedule) builder = builder.withArrearsConfiguration();

        final String loanProductJSON = builder.build(chargeId);
        return this.loanTransactionHelper.getLoanProductId(loanProductJSON);
    }
    
    private List getDateAsArray(Calendar date, int addPeriod) {
        return getDateAsArray(date, addPeriod, Calendar.DAY_OF_MONTH);
    }

    private List getDateAsArray(Calendar date, int addvalue, int type) {
        date.add(type, addvalue);
        return new ArrayList<>(Arrays.asList(date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1,
                        date.get(Calendar.DAY_OF_MONTH)));
    }
}
