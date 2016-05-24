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
package org.apache.fineract.portfolio.loanaccount.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.apache.fineract.portfolio.tax.domain.TaxComponent;
import org.apache.fineract.portfolio.tax.service.TaxUtils;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.joda.time.LocalDate;
import org.springframework.data.jpa.domain.AbstractPersistable;

@Entity
@Table(name = "m_loan_charge_paid_by")
public class LoanChargePaidBy extends AbstractPersistable<Long> {

    @ManyToOne
    @JoinColumn(name = "loan_transaction_id", nullable = false)
    private LoanTransaction loanTransaction;

    @ManyToOne(cascade= CascadeType.ALL)
    @JoinColumn(name = "loan_charge_id", nullable = false)
    private LoanCharge loanCharge;

    @Column(name = "amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal amount;

    @Column(name = "installment_number", nullable = true)
    private Integer installmentNumber;
    
    @LazyCollection(LazyCollectionOption.FALSE)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "loan_charge_paid_by_id", referencedColumnName = "id", nullable = false)
    private final List<LoanChargeTaxDetailsPaidBy> taxDetails = new ArrayList<>();

    protected LoanChargePaidBy() {

    }

    public LoanChargePaidBy(final LoanTransaction loanTransaction, final LoanCharge loanCharge, final BigDecimal amount,
            Integer installmentNumber) {
        this.loanTransaction = loanTransaction;
        this.loanCharge = loanCharge;
        this.amount = amount;
        this.installmentNumber = installmentNumber;
        
        createLoanChargeTaxDetailsPaidBy(loanCharge.getLoan().getDisbursementDate());
    }

    public LoanTransaction getLoanTransaction() {
        return this.loanTransaction;
    }

    public void setLoanTransaction(final LoanTransaction loanTransaction) {
        this.loanTransaction = loanTransaction;
    }

    public LoanCharge getLoanCharge() {
        return this.loanCharge;
    }

    public void setLoanCharge(final LoanCharge loanCharge) {
        this.loanCharge = loanCharge;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public void setAmount(final BigDecimal amount) {
        this.amount = amount;
    }

    
    public Integer getInstallmentNumber() {
        return this.installmentNumber;
    }
    
    public List<LoanChargeTaxDetailsPaidBy> getLoanChargeTaxDetailsPaidBy(){
        return this.taxDetails;
    }
    
    public void createLoanChargeTaxDetailsPaidBy(final LocalDate transactionDate) {
        if(this.loanCharge.getTaxGroup() != null && this.amount.compareTo(BigDecimal.ZERO) == 1){
            BigDecimal incomeAmount = BigDecimal.ZERO;
            Map<TaxComponent, BigDecimal> taxDetails = TaxUtils.splitTaxForLoanCharge(this.amount, transactionDate, this.loanCharge.getTaxGroup().getTaxGroupMappings(), this.amount.scale());
            BigDecimal totalTax = TaxUtils.totalTaxAmount(taxDetails);
            if(totalTax.compareTo(BigDecimal.ZERO) == 1){
                incomeAmount = this.amount;
                for (Map.Entry<TaxComponent, BigDecimal> mapEntry : taxDetails.entrySet()) {
                    this.getLoanChargeTaxDetailsPaidBy().add(new LoanChargeTaxDetailsPaidBy(mapEntry.getKey(), mapEntry.getValue()));
                    incomeAmount = incomeAmount.subtract(mapEntry.getValue());
                }
            }
        }
    }
}
