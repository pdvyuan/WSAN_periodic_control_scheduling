function values = find_all_2_5_multiples(maxvalue)
    values = [];
    for i=1:maxvalue
        v = i;
        if (mod(maxvalue, v) == 0)
            while (mod(v, 2) == 0 && v ~= 1)
                v = v / 2;
            end

            while (mod(v, 5) == 0 && v ~= 1)
                v = v / 5;
            end
            if (v == 1)
                values = [values, i];
            end
        end
    end
end