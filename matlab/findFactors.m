function series = findFactors(val)
    series = [];
    for i=1:val
        if (mod(val, i) == 0)
            series = [series, i];
        end
end