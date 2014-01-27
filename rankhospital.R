rankhospital <- function(state, outcome, num = "best") {
  ## Read outcome data
  ## Check that state and outcome are valid
  ## Return hospital name in that state with the given rank
  ## 30-day death rate

outcome1 <- read.csv("ProgAssignment3-data/outcome-of-care-measures.csv", colClasses="character" )
non1<-!is.na(state) #For checking if the sate is NA or not 
non2<-!is.na(outcome) # For checking id outcome is valid or not
out<-outcome=="heart attack" || outcome== "heart failure" || outcome=="pneumonia" 

# For checking if outcome is one among the above mentioned areas
if(non1 && state %in% names(table(outcome1$State))) # To check if state is one among the mentioned states in outcome csv 
{
  
  if(non2 && out){
    outcome1[,11]<-as.numeric(outcome1[,11]) # while importing we imported csv with colclasses as character, 
    outcome1[,17]<-as.numeric(outcome1[,17]) # so changing numbers back to numbers
    outcome1[,23]<-as.numeric(outcome1[,23])
    z<-outcome1[,7]==state # To check for rows with "State" as state
    outcome2<-outcome1[z,] 
    z<-complete.cases(outcome2)
    outcome2<-outcome2[z,]   # For cases with non NA values

    if(num=="best"){
      num<-1
    }
    else if(num=="worst"){
      num<-length(outcome2[,1])   
    }
    
    if(length(outcome1[,1])>=num){
    
    if(outcome=="heart attack"){ 
      sort.outcome<-outcome2[order(outcome2[,11],outcome2[,2]),] # Sorting by heart attack and then name of the hotel
      return(sort.outcome[num,2])
    }
    if(outcome=="heart failure"){
      sort.outcome<-outcome2[order(outcome2[,17],outcome2[,2]),] # Sorting by heart attack and then name of the hotel
      return(sort.outcome[num,2])
    }
    if(outcome=="pneumonia"){
      sort.outcome<-outcome2[order(outcome2[,23],outcome2[,2]),] # Sorting by heart attack and then name of the hotel
      return(sort.outcome[num,2])
    }
    
    }
    else{
      return(NA)
    }
  }
  else
    stop("Invalid outcome") # Error throw
}
else
  stop("Invalid State") # Errow Throw

}
