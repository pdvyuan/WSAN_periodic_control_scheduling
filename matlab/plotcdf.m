function plotcdf(pdata)
    [x, ids] = sort(pdata(:, 1));
    pdata = pdata(ids, :);
    x = zeros(size(pdata)+[1, 0]);
    x(1, 1) = 0;
    x(1, 2) = 0;
    for i = 1:size(pdata, 1)
        x(i+1, 1) = pdata(i, 1);
        x(i+1, 2) = sum(pdata(1:i, 2))/i;
        %x(i+1, 2) = sum(pdata(1:i, 2))/size(pdata, 1);
    end
    plot(x(:, 1), x(:, 2));
    hold on;
end