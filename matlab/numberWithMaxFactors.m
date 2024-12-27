function [val, num]=numberWithMaxFactors
    num = 0;
    val = 0;
    for i=1:100000
        factors = findFactors(i);
        fprintf('%d %d\n', i, length(factors));
        if (num < length(factors))
            num = length(factors);
            val = i;
        end
    end
end