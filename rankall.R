rankall <- function(outcome, num = "best") {
  ## Read outcome data
  ## Check that state and outcome are valid
  ## For each state, find the hospital of the given rank
  ## Return a data frame with the hospital names and the
  ## (abbreviated) state name

  outcome1 <- read.csv("ProgAssignment3-data/outcome-of-care-measures.csv", colClasses="character" )
  non<-!is.na(outcome) # For checking id outcome is valid or not
  out<-outcome=="heart attack" || outcome== "heart failure" || outcome=="pneumonia" 
  #numcheck<-num=="best" || num=="worst" || num > 0
  
  # For checking if outcome is one among the above mentioned areas  
    if(non && out){  # && numcheck
      outcome1[,11]<-as.numeric(outcome1[,11]) # while importing we imported csv with colclasses as character, 
      outcome1[,17]<-as.numeric(outcome1[,17]) # so changing numbers back to numbers
      outcome1[,23]<-as.numeric(outcome1[,23])
      num2<-FALSE
      
      if(num=="worst"){
        num2<-TRUE
        num1<-1
      }
      else if(num=="best")
      {
        num1<-1
      }
      else{
        num1<-num
      }
      
      if(length(outcome1[,1])>=num1){
        
        if(outcome=="heart attack"){ 
          z<-is.na(outcome1[,11])
          outcome1<-outcome1[!z,]   # For cases with non NA values
          
          state.data<-outcome1[order(outcome1[,11],outcome1[,2],decreasing=num2,na.last=NA),]
          #state.data<-state.data[order(state.data[,2]),]
          state.list<-split(state.data,state.data[,7])
          temp<-vector()
          for(i in 1:length(state.list)){
            temp<-append(temp,state.list[[c(i,2)]][num1])
          }
          data<-data.frame(hospital=temp,state=names(state.list))
          return(data)
        }
        
        if(outcome=="heart failure"){ 
         #z<-is.na(outcome1[,17])
         #  outcome1<-outcome1[!z,]   # For cases with non NA values
          state.data<-outcome1[order(outcome1[,17],outcome1[,2],decreasing=num2,na.last=NA),]
          #state.data<-state.data[order(state.data[,2]),]
          state.list<-split(state.data,state.data[,7])
          temp<-vector()
          for(i in 1:length(state.list)){
            temp<-append(temp,state.list[[c(i,2)]][num1])
          }
          data<-data.frame(hospital=temp,state=names(state.list))
          return(data)
        }
        
        if(outcome=="pneumonia"){ 
          #z<-is.na(outcome1[,23])
          #outcome1<-outcome1[!z,]   # For cases with non NA values
          state.data<-outcome1[order(outcome1[,23],outcome1[,2],decreasing=num2,na.last=NA),]
         # state.data<-state.data[order(state.data[,2]),]
          state.list<-split(state.data,state.data[,7])
          temp<-vector()
          for(i in 1:length(state.list)){
            temp<-append(temp,state.list[[c(i,2)]][num1])
          }
          data<-data.frame(hospital=temp,state=names(state.list))
          return(data)
        }
        
      }
      else{
        return(NA)
      }
    }
}
